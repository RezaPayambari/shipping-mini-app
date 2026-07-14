package com.picard.shipping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String externalOrderNumber,
        @Valid @NotNull AddressDto deliveryAddress,
        @Valid @NotEmpty List<OrderPositionDto> positions
) {}
