package com.telcocrm.paymentservice.dto.request;

import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotBlank
        String paymentRequestId,

        @NotNull
        UUID orderId,

        @NotNull
        UUID customerId,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        PaymentMethod method
) {}
