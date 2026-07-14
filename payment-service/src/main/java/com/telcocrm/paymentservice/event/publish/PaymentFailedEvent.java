package com.telcocrm.paymentservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    String reason
) {
    public static PaymentFailedEvent of(UUID orderId, String reason) {
        return new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, reason);
    }
}
