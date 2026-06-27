package com.telcocrm.orderservice.dto.request;

import com.telcocrm.orderservice.entity.enums.OrderItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(

        @NotBlank(message = "Product code must not be blank")
        String productCode,

        @NotNull(message = "Product type must not be null")
        OrderItemType productType,

        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {}