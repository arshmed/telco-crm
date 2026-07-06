package com.telcocrm.orderservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID customerId,
    UUID subscriptionId,
    String email,
    String firstName,
    String lastName
) {
    public static OrderConfirmedEvent of(UUID orderId, UUID customerId, UUID subscriptionId,
                                         String email, String firstName, String lastName) {
        return new OrderConfirmedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, subscriptionId,
                email, firstName, lastName);
    }
}
