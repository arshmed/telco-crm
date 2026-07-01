package com.telcocrm.productcatalogservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.productcatalogservice.entity.OutboxEvent;
import com.telcocrm.productcatalogservice.exception.OutboxPersistenceException;
import com.telcocrm.productcatalogservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String aggregateType, String aggregateId, String type, Object payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .type(type)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new OutboxPersistenceException(type, aggregateId, e);
        }
    }
}
