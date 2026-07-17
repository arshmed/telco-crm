package com.telcocrm.identityservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.identityservice.entity.OutboxEvent;
import com.telcocrm.identityservice.exception.OutboxPersistenceException;
import com.telcocrm.identityservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event: {} for aggregateId: {}", topic, aggregateId, e);
            throw new OutboxPersistenceException(topic, aggregateId, e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .topic(topic)
                .payload(payload)
                .createdAt(Instant.now())
                .build();

        try {
            outboxRepository.save(outboxEvent);
        } catch (DataAccessException e) {
            log.error("Failed to persist outbox event: {} for aggregateId: {}", topic, aggregateId, e);
            throw new OutboxPersistenceException(topic, aggregateId, e);
        }
    }
}
