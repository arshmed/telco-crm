package com.telcocrm.paymentservice.event.consume;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO: subscription-service henüz yazılmadı, bu event'in gerçek şeması doğrulanamadı
public record SubscriptionActivationFailedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    String reason
) {}
