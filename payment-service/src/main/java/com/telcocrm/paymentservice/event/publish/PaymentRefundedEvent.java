package com.telcocrm.paymentservice.event.publish;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID paymentId,
    BigDecimal refundedAmount
) {
    public static PaymentRefundedEvent of(UUID orderId, UUID paymentId, BigDecimal refundedAmount) {
        return new PaymentRefundedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, paymentId, refundedAmount);
    }
}
