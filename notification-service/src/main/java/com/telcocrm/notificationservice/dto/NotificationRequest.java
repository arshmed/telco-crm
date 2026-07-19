package com.telcocrm.notificationservice.dto;

import com.telcocrm.notificationservice.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String templateCode;

    @NotNull
    private NotificationChannel channel;

    private Map<String, Object> payload;
}
