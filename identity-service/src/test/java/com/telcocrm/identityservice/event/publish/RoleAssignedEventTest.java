package com.telcocrm.identityservice.event.publish;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAssignedEventTest {

    @Test
    void of_shouldPopulateEventIdAndOccurredAtAndCarryFields() {
        UUID userId = UUID.randomUUID();

        var event = RoleAssignedEvent.of(userId, "agokhan", "FIELD_DEALER");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.username()).isEqualTo("agokhan");
        assertThat(event.roleName()).isEqualTo("FIELD_DEALER");
    }

    @Test
    void of_shouldGenerateDifferentEventIdsForEachCall() {
        var userId = UUID.randomUUID();

        var first = RoleAssignedEvent.of(userId, "agokhan", "FIELD_DEALER");
        var second = RoleAssignedEvent.of(userId, "agokhan", "CALL_CENTER_AGENT");

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }
}
