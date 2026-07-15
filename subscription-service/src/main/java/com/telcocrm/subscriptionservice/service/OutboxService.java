package com.telcocrm.subscriptionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.subscriptionservice.entity.OutboxEvent;
import com.telcocrm.subscriptionservice.repository.OutboxRepository;
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

    public void saveEvent(String aggregateType, String aggregateId, String topic, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .topic(topic)
                    .payload(jsonPayload)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(event);
            log.debug("Outbox event saved: aggregateType={}, topic={}", aggregateType, topic);
        } catch (Exception e) {
            log.error("Failed to save outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
