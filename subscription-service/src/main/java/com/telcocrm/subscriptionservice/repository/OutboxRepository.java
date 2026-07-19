package com.telcocrm.subscriptionservice.repository;

import com.telcocrm.subscriptionservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
