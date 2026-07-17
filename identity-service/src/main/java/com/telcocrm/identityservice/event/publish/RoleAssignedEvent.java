package com.telcocrm.identityservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoleAssignedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID userId,
        String username,
        String roleName
) {
        public static RoleAssignedEvent of(UUID userId, String username, String roleName) {
                return new RoleAssignedEvent(UUID.randomUUID(), LocalDateTime.now(), userId, username, roleName);
        }
}
