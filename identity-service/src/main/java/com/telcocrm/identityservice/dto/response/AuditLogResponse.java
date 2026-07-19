package com.telcocrm.identityservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityType,
        UUID entityId,
        String action,
        String detail,
        String performedBy,
        LocalDateTime createdAt
) {}
