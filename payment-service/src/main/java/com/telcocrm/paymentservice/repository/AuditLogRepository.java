package com.telcocrm.paymentservice.repository;

import com.telcocrm.paymentservice.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {
}
