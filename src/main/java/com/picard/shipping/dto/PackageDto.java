package com.picard.shipping.dto;

import java.util.UUID;

public record PackageDto(
        UUID id,
        String trackingCode,
        String carrier
) {}
