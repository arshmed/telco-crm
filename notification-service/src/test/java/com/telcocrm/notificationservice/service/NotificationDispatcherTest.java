package com.telcocrm.notificationservice.service;

import com.telcocrm.notificationservice.config.NotificationAuditListener;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.enums.NotificationStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private NotificationAuditListener auditListener;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Test
    void shouldDispatchToAllChannels() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("firstName", "John");

        NotificationResponse emailResponse = NotificationResponse.builder()
                .id(UUID.randomUUID()).userId(userId)
                .templateCode("TEST").channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT).sentAt(LocalDateTime.now()).build();

        when(notificationService.sendNotification(any())).thenReturn(emailResponse);

        dispatcher.dispatchFromEvent("TEST", userId, payload);

        verify(notificationService, times(NotificationChannel.values().length)).sendNotification(any());
        verify(outboxService, atLeastOnce()).saveEvent(any(), any(), any(), any());
        verify(auditListener, atLeastOnce()).logCreate(any(), any(), any());
    }

    @Test
    void shouldHandleDispatchFailureGracefully() {
        UUID userId = UUID.randomUUID();

        when(notificationService.sendNotification(any())).thenThrow(new RuntimeException("fail"));

        dispatcher.dispatchFromEvent("TEST", userId, Map.of());

        verify(notificationService, times(NotificationChannel.values().length)).sendNotification(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldSkipWhenResponseIsNull() {
        UUID userId = UUID.randomUUID();

        when(notificationService.sendNotification(any())).thenReturn(null);

        dispatcher.dispatchFromEvent("TEST", userId, Map.of());

        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldSendEmailWhenPayloadHasEmail() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("firstName", "John", "email", "test@example.com");

        NotificationResponse emailResponse = NotificationResponse.builder()
                .id(UUID.randomUUID()).userId(userId)
                .templateCode("CUSTOMER_REGISTERED").channel(NotificationChannel.EMAIL)
                .subject("Welcome John").status(NotificationStatus.SENT).sentAt(LocalDateTime.now()).build();

        when(notificationService.sendNotification(any())).thenReturn(emailResponse);

        dispatcher.dispatchFromEvent("CUSTOMER_REGISTERED", userId, payload);

        verify(emailService).sendEmail(
                eq("test@example.com"),
                eq("Welcome John"),
                eq("customer-registered"),
                eq(payload)
        );
    }

    @Test
    void shouldNotSendEmailWhenTemplateMapMissing() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("firstName", "John", "email", "test@example.com");

        NotificationResponse emailResponse = NotificationResponse.builder()
                .id(UUID.randomUUID()).userId(userId)
                .templateCode("UNKNOWN_TEMPLATE").channel(NotificationChannel.EMAIL)
                .subject("Unknown").status(NotificationStatus.SENT).sentAt(LocalDateTime.now()).build();

        when(notificationService.sendNotification(any())).thenReturn(emailResponse);

        dispatcher.dispatchFromEvent("UNKNOWN_TEMPLATE", userId, payload);

        verify(emailService, never()).sendEmail(any(), any(), any(), any());
    }
}
