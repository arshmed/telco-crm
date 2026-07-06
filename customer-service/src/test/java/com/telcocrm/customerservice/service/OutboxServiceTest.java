package com.telcocrm.customerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.customerservice.entity.OutboxEvent;
import com.telcocrm.customerservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Captor
    private ArgumentCaptor<OutboxEvent> eventCaptor;

    @Test
    void shouldSaveOutboxEvent() throws Exception {
        var event = new TestEvent("test-data");
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"data\":\"test-data\"}");

        outboxService.saveEvent("CUSTOMER", "agg-123", "customer-registered-topic", event);

        verify(outboxRepository).save(eventCaptor.capture());
        OutboxEvent saved = eventCaptor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("CUSTOMER");
        assertThat(saved.getAggregateId()).isEqualTo("agg-123");
        assertThat(saved.getTopic()).isEqualTo("customer-registered-topic");
        assertThat(saved.getPayload()).isEqualTo("{\"data\":\"test-data\"}");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowWhenSerializationFails() throws Exception {
        var event = new TestEvent("fail");
        when(objectMapper.writeValueAsString(event)).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> outboxService.saveEvent("CUSTOMER", "agg-123", "topic", event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox persistence failed for event");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDatabaseFails() throws Exception {
        var event = new TestEvent("test-data");
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        doThrow(new DataAccessException("DB error") {}).when(outboxRepository).save(any());

        assertThatThrownBy(() -> outboxService.saveEvent("CUSTOMER", "agg-123", "topic", event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outbox persistence failed for event");
    }

    record TestEvent(String data) {}
}
