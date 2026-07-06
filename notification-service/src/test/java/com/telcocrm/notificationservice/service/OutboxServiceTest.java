package com.telcocrm.notificationservice.service;

import com.telcocrm.notificationservice.dto.NotificationRequest;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.enums.NotificationStatus;
import com.telcocrm.notificationservice.event.NotificationDispatchedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private com.telcocrm.notificationservice.repository.OutboxRepository outboxRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Captor
    private ArgumentCaptor<com.telcocrm.notificationservice.entity.OutboxEvent> eventCaptor;

    @Test
    void shouldSaveEventSuccessfully() throws Exception {
        var event = NotificationDispatchedEvent.of(
                UUID.randomUUID(), UUID.randomUUID(), "TEST", "EMAIL", "SENT");

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"status\":\"SENT\"}");

        outboxService.saveEvent("NOTIFICATION", "agg-123", "notification-dispatched-topic", event);

        verify(outboxRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAggregateType()).isEqualTo("NOTIFICATION");
        assertThat(eventCaptor.getValue().getTopic()).isEqualTo("notification-dispatched-topic");
        assertThat(eventCaptor.getValue().getPayload()).isEqualTo("{\"status\":\"SENT\"}");
    }
}
