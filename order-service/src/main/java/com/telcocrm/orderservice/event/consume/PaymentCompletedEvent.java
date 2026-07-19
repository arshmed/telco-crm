package com.telcocrm.orderservice.event.consume;

import java.time.LocalDateTime;
import java.util.UUID;

// todo: diğer servislerin publish eventlerine göre eventler değişebilir
public record PaymentCompletedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID paymentId
) {}
