package com.telcocrm.usageservice.dto.response;

import com.telcocrm.usageservice.entity.Quota;

import java.time.LocalDate;
import java.util.UUID;

public record QuotaResponse(
        UUID subscriptionId,
        String msisdn,
        String tariffCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        int minutesIncluded,
        int minutesUsed,
        int minutesRemaining,
        int smsIncluded,
        int smsUsed,
        int smsRemaining,
        int dataMbIncluded,
        int dataMbUsed,
        int dataMbRemaining
) {
    public static QuotaResponse from(Quota quota) {
        return new QuotaResponse(
                quota.getSubscriptionId(),
                quota.getMsisdn(),
                quota.getTariffCode(),
                quota.getPeriodStart(),
                quota.getPeriodEnd(),
                quota.getMinutesIncluded(),
                quota.getMinutesUsed(),
                remaining(quota.getMinutesIncluded(), quota.getMinutesUsed()),
                quota.getSmsIncluded(),
                quota.getSmsUsed(),
                remaining(quota.getSmsIncluded(), quota.getSmsUsed()),
                quota.getDataMbIncluded(),
                quota.getDataMbUsed(),
                remaining(quota.getDataMbIncluded(), quota.getDataMbUsed())
        );
    }

    private static int remaining(int included, int used) {
        return Math.max(0, included - used);
    }
}
