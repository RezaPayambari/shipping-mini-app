package com.picard.shipping.dto;

import com.picard.shipping.domain.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String externalOrderNumber,
        AddressDto deliveryAddress,
        OrderStatus status,
        List<OrderPositionDto> positions,
        ShipmentSummaryDto shipment,
        Instant createdAt
) {}
