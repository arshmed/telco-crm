package com.telcocrm.identityservice.repository;

import com.telcocrm.identityservice.entity.IdentityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityAuditLogRepository extends JpaRepository<IdentityAuditLog, UUID> {

    List<IdentityAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, UUID entityId);

    Page<IdentityAuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<IdentityAuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
}
