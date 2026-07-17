package com.telcocrm.identityservice.repository;

import com.telcocrm.identityservice.entity.IdentityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityAuditLogRepository extends JpaRepository<IdentityAuditLog, UUID> {

    List<IdentityAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, UUID entityId);
}
