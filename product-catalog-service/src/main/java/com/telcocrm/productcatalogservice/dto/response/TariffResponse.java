package com.telcocrm.productcatalogservice.dto.response;

import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record TariffResponse(
        UUID id,
        String code,
        Integer version,
        boolean current,
        String name,
        TariffType type,
        TariffSegment segment,
        BigDecimal monthlyFee,
        String currency,
        Integer minutesIncluded,
        Integer smsIncluded,
        Integer dataMbIncluded,
        TariffStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Set<AddonResponse> addons,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
