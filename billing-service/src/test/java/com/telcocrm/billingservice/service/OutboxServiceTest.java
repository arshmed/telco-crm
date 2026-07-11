package com.telcocrm.billingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.billingservice.entity.OutboxEvent;
import com.telcocrm.billingservice.exception.OutboxPersistenceException;
import com.telcocrm.billingservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void saveEvent_persistsOutboxEvent() {
        Map<String, String> payload = Map.of("key", "value");
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        outboxService.saveEvent("INVOICE", "agg-123", "invoice-topic", payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("INVOICE");
        assertThat(saved.getAggregateId()).isEqualTo("agg-123");
        assertThat(saved.getTopic()).isEqualTo("invoice-topic");
        assertThat(saved.getPayload()).contains("\"key\"").contains("\"value\"");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void saveEvent_serializesToJson() {
        Map<String, Object> payload = Map.of("name", "test", "amount", 100);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        outboxService.saveEvent("INVOICE", "agg-1", "topic", payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"name\":\"test\"").contains("\"amount\":100");
    }

    @Test
    void saveEvent_persistenceFails_throwsOutboxPersistenceException() {
        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenThrow(new DataAccessException("DB error") {});

        assertThatThrownBy(() ->
                outboxService.saveEvent("INVOICE", "agg-1", "topic", Map.of("key", "value")))
                .isInstanceOf(OutboxPersistenceException.class)
                .hasMessageContaining("topic")
                .hasMessageContaining("agg-1");
    }

    @Test
    void saveEvent_unserializableObject_throwsOutboxPersistenceException() {
        ObjectMapper strictMapper = mock(ObjectMapper.class, withSettings().defaultAnswer(
                invocation -> {
                    if (invocation.getMethod().getName().equals("writeValueAsString")) {
                        throw new com.fasterxml.jackson.core.JsonProcessingException("fail") {};
                    }
                    return null;
                }));

        OutboxService serviceWithStrictMapper = new OutboxService(outboxRepository, strictMapper);

        assertThatThrownBy(() ->
                serviceWithStrictMapper.saveEvent("INVOICE", "agg-1", "topic", new Object()))
                .isInstanceOf(OutboxPersistenceException.class);
    }

    @Test
    void saveEvent_calledTwice_persistsBoth() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        outboxService.saveEvent("INVOICE", "agg-1", "topic-1", Map.of("a", 1));
        outboxService.saveEvent("INVOICE", "agg-2", "topic-2", Map.of("b", 2));

        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }
}
