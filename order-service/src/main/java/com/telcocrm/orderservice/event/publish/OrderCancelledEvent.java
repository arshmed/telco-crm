package com.telcocrm.orderservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID customerId,
    String cancellationReason,
    String email,
    String firstName,
    String lastName
) {
    public static OrderCancelledEvent of(UUID orderId, UUID customerId, String cancellationReason,
                                         String email, String firstName, String lastName) {
        return new OrderCancelledEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, cancellationReason,
                email, firstName, lastName);
    }
}
