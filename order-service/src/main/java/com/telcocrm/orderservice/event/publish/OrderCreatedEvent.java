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
    String currency
) {
    public static OrderCreatedEvent of(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency) {
        return new OrderCreatedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, customerId, totalAmount, currency);
    }
}
