package com.picard.shipping.dto;

import com.picard.shipping.domain.ShipmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID orderId,
        String externalOrderNumber,
        ShipmentStatus status,
        Instant shippedAt,
        List<PackageDto> packages
) {}
