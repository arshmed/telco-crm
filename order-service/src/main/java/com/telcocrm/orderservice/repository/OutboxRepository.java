package com.telcocrm.orderservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.orderservice.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {}
