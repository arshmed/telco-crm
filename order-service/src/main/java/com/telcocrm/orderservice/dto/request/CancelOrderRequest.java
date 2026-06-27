package com.telcocrm.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(

        @NotBlank(message = "Cancellation reason must not be blank")
        String reason
) {}