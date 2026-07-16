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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserCommunicationPreferenceRepository preferenceRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        if (!isUserOptedIn(request.getUserId(), request.getChannel())) {
            log.info("User {} has opted out of {}, notification skipped", request.getUserId(), request.getChannel());
            return null;
        }

        NotificationTemplate template = findTemplate(request.getTemplateCode(), request.getChannel());

        String body = renderTemplate(template.getBodyTemplate(), request.getPayload());
        String subject = template.getSubject() != null
                ? renderTemplate(template.getSubject(), request.getPayload())
                : null;

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .templateCode(request.getTemplateCode())
                .channel(request.getChannel())
                .subject(subject)
                .body(body)
                .payloadJson(request.getPayload())
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);

        dispatch(saved);

        return notificationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotificationHistory(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    private boolean isUserOptedIn(UUID userId, NotificationChannel channel) {
        return preferenceRepository.findByUserIdAndChannel(userId, channel)
                .map(UserCommunicationPreference::getOptIn)
                .orElse(true);
    }

    @Cacheable(cacheNames = "notification-templates", key = "#code + ':' + #channel")
    public NotificationTemplate findTemplate(String code, NotificationChannel channel) {
        return templateRepository
                .findByCodeAndChannel(code, channel)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate",
                        "code+channel", code + "/" + channel));
    }

    private String renderTemplate(String template, Map<String, Object> payload) {
        if (payload == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    private void dispatch(Notification notification) {
        try {
            log.info("Dispatching notification {} via {} to user {}: {}",
                    notification.getId(), notification.getChannel(),
                    notification.getUserId(), notification.getBody());

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to dispatch notification {}: {}", notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }
}
