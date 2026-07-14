package com.picard.shipping.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String street,
        @NotBlank String zipCode,
        @NotBlank String city,
        @NotBlank String country
) {}
