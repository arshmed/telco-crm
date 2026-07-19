package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.dto.response.AuditLogResponse;
import com.telcocrm.identityservice.entity.IdentityAuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(IdentityAuditLog auditLog);
}
