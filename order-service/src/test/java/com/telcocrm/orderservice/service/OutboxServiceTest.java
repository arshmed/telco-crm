package com.telcocrm.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.orderservice.entity.OutboxEvent;
import com.telcocrm.orderservice.exception.OutboxPersistenceException;
import com.telcocrm.orderservice.repository.OutboxRepository;
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

    record TestEvent(String data) {}

    @Test
    void shouldSaveOutboxEvent() throws Exception {
        var event = new TestEvent("test-data");
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"data\":\"test-data\"}");

        outboxService.saveEvent("ORDER", "order-123", "order-created-topic", event);

        verify(outboxRepository).save(eventCaptor.capture());
        OutboxEvent saved = eventCaptor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("ORDER");
        assertThat(saved.getAggregateId()).isEqualTo("order-123");
        assertThat(saved.getTopic()).isEqualTo("order-created-topic");
        assertThat(saved.getPayload()).isEqualTo("{\"data\":\"test-data\"}");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowWhenSerializationFails() throws Exception {
        var event = new TestEvent("fail");
        when(objectMapper.writeValueAsString(event)).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> outboxService.saveEvent("ORDER", "order-123", "topic", event))
                .isInstanceOf(OutboxPersistenceException.class)
                .hasMessageContaining("order-123");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDatabaseFails() throws Exception {
        var event = new TestEvent("test-data");
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        doThrow(new DataAccessException("DB error") {}).when(outboxRepository).save(any());

        assertThatThrownBy(() -> outboxService.saveEvent("ORDER", "order-123", "topic", event))
                .isInstanceOf(OutboxPersistenceException.class)
                .hasMessageContaining("order-123");
    }
}
