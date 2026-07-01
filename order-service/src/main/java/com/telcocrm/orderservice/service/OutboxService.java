package com.telcocrm.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.orderservice.entity.OutboxEvent;
import com.telcocrm.orderservice.exception.OutboxPersistenceException;
import com.telcocrm.orderservice.repository.OutboxRepository;
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

    public void saveEvent(String aggregateType, String aggregateId, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .topic(topic)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxEvent);

        } catch (Exception e) {
            log.error("Failed to save outbox event: {} for aggregateId: {}", topic, aggregateId, e);
            throw new OutboxPersistenceException(topic, aggregateId, e);
        }
    }
}
