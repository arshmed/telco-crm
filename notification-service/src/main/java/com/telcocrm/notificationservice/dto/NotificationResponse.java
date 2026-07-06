package com.telcocrm.notificationservice.dto;

import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.enums.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String templateCode;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private NotificationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
