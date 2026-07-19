package com.telcocrm.ticketservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.ticketservice.entity.OutboxEvent;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.event.publish.SlaBreachedEvent;
import com.telcocrm.ticketservice.event.publish.TicketEventTopics;
import com.telcocrm.ticketservice.exception.OutboxPersistenceException;
import com.telcocrm.ticketservice.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private SlaBreachedEvent sampleEvent() {
        return SlaBreachedEvent.of(UUID.randomUUID(), UUID.randomUUID(), TicketPriority.URGENT,
                "tech-support", LocalDateTime.now());
    }

    @Test
    void shouldPersistSerializedEvent() {
        var service = new OutboxService(outboxRepository, objectMapper);
        var event = sampleEvent();
        String aggregateId = event.ticketId().toString();

        service.saveEvent(TicketEventTopics.AGGREGATE_TYPE, aggregateId, TicketEventTopics.SLA_BREACHED, event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("TICKET");
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getTopic()).isEqualTo("sla-breached-topic");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPayload()).contains(aggregateId).contains("URGENT");
    }

    @Test
    void shouldThrowWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        var service = new OutboxService(outboxRepository, failingMapper);

        assertThatThrownBy(() -> service.saveEvent("TICKET", "agg-1", "topic", sampleEvent()))
                .isInstanceOf(OutboxPersistenceException.class)
                .hasMessageContaining("topic");

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPersistenceFails() {
        var service = new OutboxService(outboxRepository, objectMapper);
        doThrow(new DataIntegrityViolationException("db down")).when(outboxRepository).save(any());

        assertThatThrownBy(() -> service.saveEvent("TICKET", "agg-1", "topic", sampleEvent()))
                .isInstanceOf(OutboxPersistenceException.class)
                .hasMessageContaining("agg-1");
    }
}
