package com.telcocrm.orderservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID customerId,
    UUID subscriptionId
) {
    public static OrderConfirmedEvent of(UUID orderId, UUID customerId, UUID subscriptionId) {
        return new OrderConfirmedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, subscriptionId);
    }
}
