package com.telcocrm.usageservice.dto.response;

import com.telcocrm.usageservice.entity.UsageRecord;
import com.telcocrm.usageservice.entity.enums.UsageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsageRecordResponse(
        UUID id,
        UsageType type,
        int quantity,
        LocalDateTime recordedAt,
        String cdrRef
) {
    public static UsageRecordResponse from(UsageRecord record) {
        return new UsageRecordResponse(
                record.getId(),
                record.getType(),
                record.getQuantity(),
                record.getRecordedAt(),
                record.getCdrRef()
        );
    }
}
