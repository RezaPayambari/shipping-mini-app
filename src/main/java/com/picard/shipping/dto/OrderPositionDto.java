package com.picard.shipping.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderPositionDto(
        @NotBlank String sku,
        @Min(1) int quantity,
        String description
) {}
