package com.telcocrm.paymentservice.dto.request;

import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

// Not: amount/currency/customerId bilerek burada YOK — client'a güvenilmez, PaymentServiceImpl
// bunları orderId üzerinden order-service'ten (source of truth) sunucu tarafında çeker.
public record CreatePaymentRequest(

        @NotBlank
        String paymentRequestId,

        @NotNull
        UUID orderId,

        @NotNull
        PaymentMethod method,

        @NotBlank
        String cardHolder,

        @NotBlank
        @Pattern(regexp = "[0-9 ]{13,23}", message = "cardNumber must contain 13-19 digits")
        String cardNumber,

        @NotBlank
        @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}", message = "expiryDate must be in MM/YY format")
        String expiryDate,

        @NotBlank
        @Pattern(regexp = "[0-9]{3,4}", message = "cvv must be 3 or 4 digits")
        String cvv
) {}
