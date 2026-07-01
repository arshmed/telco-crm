package com.telcocrm.productcatalogservice.dto.response;

import com.telcocrm.productcatalogservice.entity.enums.AddonType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AddonResponse(
        UUID id,
        String code,
        String name,
        AddonType type,
        BigDecimal price,
        String currency,
        Integer validityDays,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
