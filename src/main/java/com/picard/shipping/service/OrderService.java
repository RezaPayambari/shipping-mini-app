package com.picard.shipping.service;

import com.picard.shipping.domain.Address;
import com.picard.shipping.domain.Order;
import com.picard.shipping.domain.OrderPosition;
import com.picard.shipping.dto.*;
import com.picard.shipping.exception.ConflictException;
import com.picard.shipping.exception.NotFoundException;
import com.picard.shipping.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderService
{

    private final OrderRepository orderRepository;

    public OrderResponse createOrder(CreateOrderRequest request)
    {
        if (orderRepository.existsByExternalOrderNumber(request.externalOrderNumber()))
        {
            throw new ConflictException(
                "Order with externalOrderNumber '" + request.externalOrderNumber() + "' already exists");
        }

        Order order = new Order();
        order.setExternalOrderNumber(request.externalOrderNumber());
        order.setDeliveryAddress(new Address(
            request.deliveryAddress().street(),
            request.deliveryAddress().zipCode(),
            request.deliveryAddress().city(),
            request.deliveryAddress().country()
        ));

        for (OrderPositionDto positionDto : request.positions())
        {
            OrderPosition position = new OrderPosition();
            position.setSku(positionDto.sku());
            position.setQuantity(positionDto.quantity());
            position.setDescription(positionDto.description());
            order.addPosition(position);
        }

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id)
    {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Order with id {} not found", id);
                return new NotFoundException("Order not found: " + id);
            });
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders()
    {
        return orderRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(UUID id)
    {
        return orderRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Order with id {} not found", id);
                return new NotFoundException("Order not found: " + id);
            });
    }

    private OrderResponse toResponse(Order order)
    {
        List<OrderPositionDto> positions = order.getPositions().stream()
            .map(p -> new OrderPositionDto(p.getSku(), p.getQuantity(), p.getDescription()))
            .toList();

        ShipmentSummaryDto shipmentSummary = order.getShipment() == null ? null
            : new ShipmentSummaryDto(order.getShipment().getId(), order.getShipment().getStatus());

        AddressDto addressDto = new AddressDto(
            order.getDeliveryAddress().getStreet(),
            order.getDeliveryAddress().getZipCode(),
            order.getDeliveryAddress().getCity(),
            order.getDeliveryAddress().getCountry()
        );

        return new OrderResponse(
            order.getId(),
            order.getExternalOrderNumber(),
            addressDto,
            order.getStatus(),
            positions,
            shipmentSummary,
            order.getCreatedAt()
        );
    }
}
