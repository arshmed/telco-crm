package com.telcocrm.productcatalogservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.productcatalogservice.entity.OutboxEvent;
import com.telcocrm.productcatalogservice.exception.OutboxPersistenceException;
import com.telcocrm.productcatalogservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxServiceTest {

    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);

    @Test
    void publishPersistsSerializedEvent() {
        OutboxService service = new OutboxService(outboxRepository, new ObjectMapper());

        service.publish("Tariff", "agg-1", "TariffCreated", Map.of("code", "GNC-20GB"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals("Tariff", saved.getAggregateType());
        assertEquals("agg-1", saved.getAggregateId());
        assertEquals("TariffCreated", saved.getType());
        assertEquals("{\"code\":\"GNC-20GB\"}", saved.getPayload());
    }

    @Test
    void publishWrapsSerializationFailure() throws JsonProcessingException {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("boom") {});
        OutboxService service = new OutboxService(outboxRepository, failing);

        assertThrows(OutboxPersistenceException.class,
                () -> service.publish("Tariff", "agg-1", "TariffCreated", new Object()));
    }
}
