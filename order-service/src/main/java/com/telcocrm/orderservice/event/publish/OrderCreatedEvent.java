package com.telcocrm.orderservice.event.publish;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    String email,
    String firstName,
    String lastName,
    String tariffCode
) {
    public static OrderCreatedEvent of(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency,
                                       String email, String firstName, String lastName, String tariffCode) {
        return new OrderCreatedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, totalAmount, currency,
                email, firstName, lastName, tariffCode);
    }
}
