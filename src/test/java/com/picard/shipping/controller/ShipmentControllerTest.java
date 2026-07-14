package com.picard.shipping.controller;

import com.picard.shipping.domain.ShipmentStatus;
import com.picard.shipping.dto.*;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ShipmentService shipmentService;

    private ShipmentResponse sampleResponse() {
        return new ShipmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORD-001",
                ShipmentStatus.CREATED,
                null,
                List.of()
        );
    }

    @Test
    void createShipmentForOrder_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        given(shipmentService.createShipmentForOrder(orderId)).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/shipments/order/{orderId}", orderId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getShipment_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(shipmentService.getShipment(id)).willThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/api/v1/shipments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsShipmentList() throws Exception {
        given(shipmentService.search(null, null)).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void pack_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ShipmentResponse packed = new ShipmentResponse(id, UUID.randomUUID(), "ORD-001",
                ShipmentStatus.PACKED, null, List.of());
        given(shipmentService.markPacked(id)).willReturn(packed);

        mockMvc.perform(post("/api/v1/shipments/{id}/pack", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"));
    }

    @Test
    void labelPackage_validRequest_returns200() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        given(shipmentService.labelPackage(eq(shipmentId), eq(packageId), any()))
                .willReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/shipments/{shipmentId}/packages/{packageId}", shipmentId, packageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingCode": "TRACK-001", "carrier": "DHL"}
                                """))
                .andExpect(status().isOk());
    }
}
