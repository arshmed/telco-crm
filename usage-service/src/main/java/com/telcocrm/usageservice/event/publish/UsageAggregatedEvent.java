package com.telcocrm.usageservice.event.publish;

import com.telcocrm.usageservice.entity.Quota;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsageAggregatedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID subscriptionId,
        UUID customerId,
        String tariffCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        int overageMinutes,
        int overageSms,
        int overageDataMb
) {
    public static UsageAggregatedEvent of(Quota quota) {
        return new UsageAggregatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                quota.getSubscriptionId(),
                quota.getCustomerId(),
                quota.getTariffCode(),
                quota.getPeriodStart(),
                quota.getPeriodEnd(),
                overage(quota.getMinutesUsed(), quota.getMinutesIncluded()),
                overage(quota.getSmsUsed(), quota.getSmsIncluded()),
                overage(quota.getDataMbUsed(), quota.getDataMbIncluded())
        );
    }

    private static int overage(int used, int included) {
        return Math.max(0, used - included);
    }
}
