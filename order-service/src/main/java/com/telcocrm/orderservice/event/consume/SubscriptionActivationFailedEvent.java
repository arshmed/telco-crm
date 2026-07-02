package com.telcocrm.orderservice.event.consume;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionActivationFailedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    String reason
) {}
