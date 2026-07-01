package com.telcocrm.customerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.customerservice.entity.OutboxEvent;
import com.telcocrm.customerservice.enums.OutboxStatus;
import com.telcocrm.customerservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(json)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to persist outbox event {} for aggregateId {}", eventType, aggregateId, e);
            throw new RuntimeException("Outbox persistence failed for event: " + eventType, e);
        }
    }
}
