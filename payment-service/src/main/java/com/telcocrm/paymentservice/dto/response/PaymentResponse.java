package com.telcocrm.paymentservice.dto.response;

import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String externalRef,
        String failureReason,
        Instant paidAt,
        List<PaymentAttemptResponse> attempts,
        Instant createdAt,
        Instant updatedAt
) {}
