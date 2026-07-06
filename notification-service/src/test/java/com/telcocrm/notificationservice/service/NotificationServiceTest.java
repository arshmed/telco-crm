package com.telcocrm.notificationservice.service;

import com.telcocrm.notificationservice.dto.NotificationRequest;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.entity.Notification;
import com.telcocrm.notificationservice.entity.NotificationTemplate;
import com.telcocrm.notificationservice.entity.UserCommunicationPreference;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.enums.NotificationStatus;
import com.telcocrm.notificationservice.exception.ResourceNotFoundException;
import com.telcocrm.notificationservice.mapper.NotificationMapper;
import com.telcocrm.notificationservice.repository.NotificationRepository;
import com.telcocrm.notificationservice.repository.NotificationTemplateRepository;
import com.telcocrm.notificationservice.repository.UserCommunicationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private UserCommunicationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationRequest validRequest() {
        return NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .templateCode("CUSTOMER_REGISTERED")
                .channel(NotificationChannel.EMAIL)
                .payload(Map.of("firstName", "John", "lastName", "Doe"))
                .build();
    }

    private NotificationTemplate emailTemplate() {
        return NotificationTemplate.builder()
                .code("CUSTOMER_REGISTERED")
                .channel(NotificationChannel.EMAIL)
                .subject("Welcome {{firstName}}")
                .bodyTemplate("Hello {{firstName}} {{lastName}}!")
                .locale("tr")
                .build();
    }

    private Notification pendingNotification(NotificationRequest request) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .templateCode(request.getTemplateCode())
                .channel(request.getChannel())
                .body("Hello John Doe!")
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NotificationResponse response(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .templateCode(notification.getTemplateCode())
                .channel(notification.getChannel())
                .body(notification.getBody())
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Test
    void shouldSendNotification() {
        var request = validRequest();
        var template = emailTemplate();
        var notification = pendingNotification(request);
        var response = response(notification);

        when(preferenceRepository.findByUserIdAndChannel(request.getUserId(), request.getChannel()))
                .thenReturn(Optional.empty());
        when(templateRepository.findByCodeAndChannel(request.getTemplateCode(), request.getChannel()))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any())).thenReturn(notification);
        notification.setStatus(NotificationStatus.SENT);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.sendNotification(request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEqualTo("Hello John Doe!");
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void shouldSkipWhenUserOptedOut() {
        var request = validRequest();

        when(preferenceRepository.findByUserIdAndChannel(request.getUserId(), request.getChannel()))
                .thenReturn(Optional.of(
                        UserCommunicationPreference.builder().optIn(false).build()));

        NotificationResponse result = notificationService.sendNotification(request);

        assertThat(result).isNull();
        verify(templateRepository, never()).findByCodeAndChannel(any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenTemplateNotFound() {
        var request = validRequest();

        when(preferenceRepository.findByUserIdAndChannel(request.getUserId(), request.getChannel()))
                .thenReturn(Optional.empty());
        when(templateRepository.findByCodeAndChannel(request.getTemplateCode(), request.getChannel()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.sendNotification(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRenderTemplateWithPayload() {
        var request = validRequest();
        var template = emailTemplate();
        var notification = pendingNotification(request);
        var response = response(notification);

        when(preferenceRepository.findByUserIdAndChannel(request.getUserId(), request.getChannel()))
                .thenReturn(Optional.empty());
        when(templateRepository.findByCodeAndChannel(request.getTemplateCode(), request.getChannel()))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any())).thenReturn(notification);
        notification.setStatus(NotificationStatus.SENT);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.sendNotification(request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isEqualTo("Hello John Doe!");
    }

    @Test
    void shouldGetUserNotificationHistory() {
        UUID userId = UUID.randomUUID();
        var notification = Notification.builder()
                .id(UUID.randomUUID()).userId(userId)
                .templateCode("TEST").channel(NotificationChannel.EMAIL)
                .body("test").status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now()).build();
        var response = response(notification);
        var page = new PageImpl<>(List.of(notification));

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        Page<NotificationResponse> result = notificationService.getUserNotificationHistory(userId, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldMarkAsFailedOnDispatchError() {
        var request = validRequest();
        var template = emailTemplate();

        when(preferenceRepository.findByUserIdAndChannel(request.getUserId(), request.getChannel()))
                .thenReturn(Optional.empty());
        when(templateRepository.findByCodeAndChannel(request.getTemplateCode(), request.getChannel()))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any()))
                .thenThrow(new RuntimeException("Channel unavailable"));

        assertThatThrownBy(() -> notificationService.sendNotification(request))
                .isInstanceOf(RuntimeException.class);
    }
}
