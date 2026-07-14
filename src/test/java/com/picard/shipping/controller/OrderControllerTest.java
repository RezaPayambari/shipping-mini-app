package com.picard.shipping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picard.shipping.domain.OrderStatus;
import com.picard.shipping.dto.*;
import com.picard.shipping.exception.ConflictException;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OrderService orderService;

    private OrderResponse sampleResponse() {
        return new OrderResponse(
                UUID.randomUUID(),
                "ORD-001",
                new AddressDto("Main St 1", "12345", "Berlin", "DE"),
                OrderStatus.CREATED,
                List.of(),
                null,
                Instant.now()
        );
    }

    @Test
    void createOrderValidRequestReturns201() throws Exception {
        given(orderService.createOrder(any())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderNumber": "ORD-001",
                                  "deliveryAddress": {
                                    "street": "Main St 1",
                                    "zipCode": "12345",
                                    "city": "Berlin",
                                    "country": "DE"
                                  },
                                  "positions": [{"sku": "SKU-A", "quantity": 2}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalOrderNumber").value("ORD-001"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/orders/")));
    }

    @Test
    void createOrderMissingExternalOrderNumberReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryAddress": {
                                    "street": "Main St 1",
                                    "zipCode": "12345",
                                    "city": "Berlin",
                                    "country": "DE"
                                  },
                                  "positions": [{"sku": "SKU-A", "quantity": 2}]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderExistingIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(orderService.getOrder(id)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalOrderNumber").value("ORD-001"));
    }

    @Test
    void getOrderNotFoundReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(orderService.getOrder(id)).willThrow(new NotFoundException("Order not found: " + id));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllOrdersReturns200() throws Exception {
        given(orderService.getAllOrders()).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
