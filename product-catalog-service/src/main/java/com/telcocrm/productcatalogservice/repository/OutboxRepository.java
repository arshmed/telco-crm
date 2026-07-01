package com.telcocrm.productcatalogservice.repository;

import com.telcocrm.productcatalogservice.entity.OutboxEvent;
import com.telcocrm.productcatalogservice.entity.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
