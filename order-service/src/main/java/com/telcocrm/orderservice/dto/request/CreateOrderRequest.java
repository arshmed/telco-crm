package com.telcocrm.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "Customer ID must not be null")
        UUID customerId,

        @Valid
        @NotEmpty(message = "At least one item is required")
        List<OrderItemRequest> items
) {}