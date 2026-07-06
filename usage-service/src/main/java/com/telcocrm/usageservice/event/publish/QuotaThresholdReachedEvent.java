package com.telcocrm.usageservice.event.publish;

import com.telcocrm.usageservice.entity.enums.UsageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuotaThresholdReachedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID subscriptionId,
        UUID customerId,
        String msisdn,
        UsageType quotaType,
        int thresholdPercent,
        int used,
        int included
) {
    public static QuotaThresholdReachedEvent of(UUID subscriptionId, UUID customerId, String msisdn,
                                                UsageType quotaType, int thresholdPercent, int used, int included) {
        return new QuotaThresholdReachedEvent(UUID.randomUUID(), LocalDateTime.now(),
                subscriptionId, customerId, msisdn, quotaType, thresholdPercent, used, included);
    }
}
