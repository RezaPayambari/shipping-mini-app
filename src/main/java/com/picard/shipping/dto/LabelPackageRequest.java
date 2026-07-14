package com.picard.shipping.dto;

import jakarta.validation.constraints.NotBlank;

public record LabelPackageRequest(
        @NotBlank String trackingCode,
        @NotBlank String carrier
) {}
