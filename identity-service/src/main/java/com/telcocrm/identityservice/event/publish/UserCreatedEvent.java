package com.telcocrm.identityservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID userId,
        String username,
        String email,
        String fullName,
        UUID customerId
) {
        public static UserCreatedEvent of(UUID userId, String username, String email, String fullName, UUID customerId) {
                return new UserCreatedEvent(UUID.randomUUID(), LocalDateTime.now(), userId, username, email, fullName, customerId);
        }
}
