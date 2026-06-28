package com.telcocrm.orderservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID customerId,
    String cancellationReason
) {
    public static OrderCancelledEvent of(UUID orderId, UUID customerId, String cancellationReason) {
        return new OrderCancelledEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, cancellationReason);
    }
}
