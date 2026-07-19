package com.telcocrm.orderservice.event.consume;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionActivatedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID subscriptionId
) {}
