package com.telcocrm.notificationservice.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record NotificationDispatchedEvent(
    UUID notificationId,
    UUID userId,
    String templateCode,
    String channel,
    String status,
    LocalDateTime dispatchedAt
) {
    public static NotificationDispatchedEvent of(UUID notificationId, UUID userId,
                                                  String templateCode, String channel,
                                                  String status) {
        return NotificationDispatchedEvent.builder()
            .notificationId(notificationId)
            .userId(userId)
            .templateCode(templateCode)
            .channel(channel)
            .status(status)
            .dispatchedAt(LocalDateTime.now())
            .build();
    }
}
