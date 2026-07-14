package com.picard.shipping.service;

import com.picard.shipping.domain.Order;
import com.picard.shipping.domain.OrderStatus;
import com.picard.shipping.dto.*;
import com.picard.shipping.exception.ConflictException;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    private AddressDto addressDto() {
        return new AddressDto("Main St 1", "12345", "Berlin", "DE");
    }

    private OrderPositionDto positionDto() {
        return new OrderPositionDto("SKU-A", 2, null);
    }

    @Test
    void createOrderDuplicateExternalOrderNumberThrowsConflict() {
        given(orderRepository.existsByExternalOrderNumber("ORD-1")).willReturn(true);
        CreateOrderRequest req = new CreateOrderRequest("ORD-1", addressDto(), List.of(positionDto()));

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ORD-1");
    }

    @Test
    void createOrderHappyPathSavesAndReturnsResponse() {
        given(orderRepository.existsByExternalOrderNumber("ORD-1")).willReturn(false);

        Order savedOrder = new Order();
        savedOrder.setExternalOrderNumber("ORD-1");
        savedOrder.setStatus(OrderStatus.CREATED);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        CreateOrderRequest req = new CreateOrderRequest("ORD-1", addressDto(), List.of(positionDto()));
        OrderResponse response = orderService.createOrder(req);

        assertThat(response.externalOrderNumber()).isEqualTo("ORD-1");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void getOrderUnknownIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        given(orderRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrderExistingIdReturnsResponse() {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.setExternalOrderNumber("ORD-2");
        order.setStatus(OrderStatus.CREATED);
        given(orderRepository.findById(id)).willReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(id);
        assertThat(response.externalOrderNumber()).isEqualTo("ORD-2");
    }

    @Test
    void getAllOrdersReturnsMappedList() {
        Order order = new Order();
        order.setExternalOrderNumber("ORD-3");
        order.setStatus(OrderStatus.CREATED);
        given(orderRepository.findAll()).willReturn(List.of(order));

        List<OrderResponse> result = orderService.getAllOrders();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalOrderNumber()).isEqualTo("ORD-3");
    }

    @Test
    void getOrderEntityUnknownIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        given(orderRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderEntity(id))
                .isInstanceOf(NotFoundException.class);
    }
}
