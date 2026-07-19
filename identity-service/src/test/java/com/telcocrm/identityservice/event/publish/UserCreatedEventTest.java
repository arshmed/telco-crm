package com.telcocrm.identityservice.event.publish;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserCreatedEventTest {

    @Test
    void of_shouldPopulateEventIdAndOccurredAtAndCarryFields() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        var event = UserCreatedEvent.of(userId, "agokhan", "agokhan@example.com", "Ahmet Gokhan", customerId);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.username()).isEqualTo("agokhan");
        assertThat(event.email()).isEqualTo("agokhan@example.com");
        assertThat(event.fullName()).isEqualTo("Ahmet Gokhan");
        assertThat(event.customerId()).isEqualTo(customerId);
    }

    @Test
    void of_shouldAllowNullCustomerId() {
        var event = UserCreatedEvent.of(UUID.randomUUID(), "staffuser", "staff@example.com", "Staff User", null);

        assertThat(event.customerId()).isNull();
    }

    @Test
    void of_shouldGenerateDifferentEventIdsForEachCall() {
        var first = UserCreatedEvent.of(UUID.randomUUID(), "u1", "u1@example.com", "U One", null);
        var second = UserCreatedEvent.of(UUID.randomUUID(), "u2", "u2@example.com", "U Two", null);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }
}
