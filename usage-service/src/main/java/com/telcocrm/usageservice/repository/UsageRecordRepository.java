package com.telcocrm.usageservice.repository;

import com.telcocrm.usageservice.entity.UsageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    Page<UsageRecord> findBySubscriptionIdAndRecordedAtBetween(
            UUID subscriptionId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
