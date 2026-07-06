package com.telcocrm.productcatalogservice.event.publish;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TariffPriceChangedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tariffId,
        String code,
        Integer newVersion,
        BigDecimal oldMonthlyFee,
        BigDecimal newMonthlyFee,
        String currency
) {
    public static TariffPriceChangedEvent of(UUID tariffId, String code, Integer newVersion,
                                             BigDecimal oldFee, BigDecimal newFee, String currency) {
        return new TariffPriceChangedEvent(UUID.randomUUID(), Instant.now(), tariffId, code, newVersion, oldFee, newFee, currency);
    }
}
