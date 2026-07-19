package com.telcocrm.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.paymentservice.entity.OutboxEvent;
import com.telcocrm.paymentservice.exception.OutboxPersistenceException;
import com.telcocrm.paymentservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void saveEvent_shouldSerializeAndPersistOutboxEvent() throws JsonProcessingException {
        Object event = new Object();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"foo\":\"bar\"}");

        outboxService.saveEvent("PAYMENT", "payment-1", "payment-completed-topic", event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(saved.getAggregateId()).isEqualTo("payment-1");
        assertThat(saved.getTopic()).isEqualTo("payment-completed-topic");
        assertThat(saved.getPayload()).isEqualTo("{\"foo\":\"bar\"}");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void saveEvent_shouldThrowOutboxPersistenceExceptionWhenSerializationFails() throws JsonProcessingException {
        Object event = new Object();
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> outboxService.saveEvent("PAYMENT", "payment-1", "payment-completed-topic", event))
                .isInstanceOf(OutboxPersistenceException.class);

        verify(outboxRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void saveEvent_shouldThrowOutboxPersistenceExceptionWhenSaveFails() throws JsonProcessingException {
        Object event = new Object();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(outboxRepository.save(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> outboxService.saveEvent("PAYMENT", "payment-1", "payment-completed-topic", event))
                .isInstanceOf(OutboxPersistenceException.class);
    }
}
