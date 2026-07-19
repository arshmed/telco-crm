package com.telcocrm.productcatalogservice.event.publish;

import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TariffCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tariffId,
        String code,
        Integer version,
        String name,
        TariffType type,
        TariffSegment segment,
        BigDecimal monthlyFee,
        String currency
) {
    public static TariffCreatedEvent of(Tariff tariff) {
        return new TariffCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                tariff.getId(),
                tariff.getCode(),
                tariff.getVersion(),
                tariff.getName(),
                tariff.getType(),
                tariff.getSegment(),
                tariff.getMonthlyFee(),
                tariff.getCurrency()
        );
    }
}
