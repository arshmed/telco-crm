package com.telcocrm.paymentservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID paymentId,
    UUID invoiceId
) {
    public static PaymentCompletedEvent of(UUID orderId, UUID paymentId, UUID invoiceId) {
        return new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, paymentId, invoiceId);
    }
}
