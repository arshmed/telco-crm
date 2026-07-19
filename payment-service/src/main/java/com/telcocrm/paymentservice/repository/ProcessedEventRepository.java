package com.telcocrm.paymentservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.paymentservice.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventId(UUID eventId);
}
