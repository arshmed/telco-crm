package com.telcocrm.productcatalogservice.repository;

import com.telcocrm.productcatalogservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
