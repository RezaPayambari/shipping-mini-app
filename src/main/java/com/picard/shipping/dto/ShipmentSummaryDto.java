package com.picard.shipping.dto;

import com.picard.shipping.domain.ShipmentStatus;

import java.util.UUID;

public record ShipmentSummaryDto(
        UUID id,
        ShipmentStatus status
) {}
