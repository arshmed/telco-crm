package com.telcocrm.usageservice.event.publish;

import com.telcocrm.usageservice.entity.enums.UsageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuotaExceededEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID subscriptionId,
        UUID customerId,
        String msisdn,
        UsageType quotaType,
        int used,
        int included,
        String email,
        String firstName,
        String lastName
) {
    public static QuotaExceededEvent of(UUID subscriptionId, UUID customerId, String msisdn,
                                        UsageType quotaType, int used, int included,
                                        String email, String firstName, String lastName) {
        return new QuotaExceededEvent(UUID.randomUUID(), LocalDateTime.now(),
                subscriptionId, customerId, msisdn, quotaType, used, included,
                email, firstName, lastName);
    }
}
