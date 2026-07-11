package com.telcocrm.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        UUID customerId,

        String customerNo,

        @Valid
        @NotEmpty(message = "At least one item is required")
        List<OrderItemRequest> items
) {}
