package com.telcocrm.usageservice.event.consume;

import com.telcocrm.usageservice.entity.enums.UsageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CdrRecordedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID subscriptionId,
        UsageType type,
        Integer quantity,
        String cdrRef
) {}
