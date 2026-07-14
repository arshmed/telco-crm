package com.telcocrm.paymentservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID paymentId
) {
    public static PaymentCompletedEvent of(UUID orderId, UUID paymentId) {
        return new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, paymentId);
    }
}
