package com.telcocrm.usageservice.event.consume;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionActivatedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID orderId,
        UUID subscriptionId,
        UUID customerId,
        String msisdn,
        String tariffCode
) {}
