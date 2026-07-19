package com.telcocrm.productcatalogservice.event.publish;

import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;

import java.time.Instant;
import java.util.UUID;

public record TariffPublishedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tariffId,
        String code,
        Integer version,
        TariffStatus status
) {
    public static TariffPublishedEvent of(Tariff tariff) {
        return new TariffPublishedEvent(
                UUID.randomUUID(),
                Instant.now(),
                tariff.getId(),
                tariff.getCode(),
                tariff.getVersion(),
                tariff.getStatus()
        );
    }
}
