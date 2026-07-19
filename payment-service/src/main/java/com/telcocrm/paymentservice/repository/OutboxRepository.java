package com.telcocrm.paymentservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.telcocrm.paymentservice.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

}
