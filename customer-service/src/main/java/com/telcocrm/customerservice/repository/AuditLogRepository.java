package com.telcocrm.customerservice.repository;

import com.telcocrm.customerservice.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {
}
