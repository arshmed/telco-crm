package com.telcocrm.notificationservice.service;

import com.telcocrm.notificationservice.config.NotificationAuditListener;
import com.telcocrm.notificationservice.dto.NotificationRequest;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.event.NotificationDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final OutboxService outboxService;
    private final NotificationAuditListener auditListener;
    private final EmailService emailService;

    private static final Map<String, String> TEMPLATE_MAP = Map.of(
        "CUSTOMER_REGISTERED", "customer-registered",
        "CUSTOMER_KYC_APPROVED", "customer-kyc-approved",
        "CUSTOMER_KYC_REJECTED", "customer-kyc-rejected",
        "CUSTOMER_UPDATED", "customer-updated",
        "ORDER_CREATED", "order-created",
        "ORDER_CONFIRMED", "order-confirmed",
        "ORDER_CANCELLED", "order-cancelled",
        "QUOTA_THRESHOLD_REACHED", "quota-threshold-reached",
        "QUOTA_EXCEEDED", "quota-exceeded"
    );

    public void dispatchFromEvent(String templateCode, UUID userId, Map<String, Object> payload) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            try {
                NotificationRequest request = NotificationRequest.builder()
                        .userId(userId)
                        .templateCode(templateCode)
                        .channel(channel)
                        .payload(payload)
                        .build();

                NotificationResponse response = notificationService.sendNotification(request);
                if (response != null) {
                    if (channel == NotificationChannel.EMAIL) {
                        sendEmail(templateCode, payload, response);
                    }
                    outboxService.saveEvent(
                            "NOTIFICATION",
                            response.getId().toString(),
                            "notification-dispatched-topic",
                            NotificationDispatchedEvent.of(
                                    response.getId(), userId, templateCode,
                                    channel.name(), response.getStatus().name()
                            )
                    );
                    auditListener.logCreate("Notification", response.getId().toString(), response);
                }
            } catch (Exception e) {
                log.warn("Failed to dispatch {} notification for template {} to user {}: {}",
                        channel, templateCode, userId, e.getMessage());
            }
        }
    }

    private void sendEmail(String templateCode, Map<String, Object> payload, NotificationResponse response) {
        String recipientEmail = (String) payload.get("email");
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No email in payload for template {}, notification {} skipped as real email",
                    templateCode, response.getId());
            return;
        }
        String emailTemplate = TEMPLATE_MAP.get(templateCode);
        if (emailTemplate == null) {
            log.warn("No HTML email template for {}, notification {} skipped as real email",
                    templateCode, response.getId());
            return;
        }
        emailService.sendEmail(
                recipientEmail,
                response.getSubject(),
                emailTemplate,
                payload
        );
    }
}
