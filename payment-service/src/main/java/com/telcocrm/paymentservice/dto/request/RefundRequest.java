package com.telcocrm.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(

        @NotBlank(message = "Refund reason must not be blank")
        String reason
) {}
