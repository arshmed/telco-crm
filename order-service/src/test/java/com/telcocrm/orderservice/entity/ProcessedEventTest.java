package com.telcocrm.orderservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventTest {

    @Test
    void shouldBuildWithAllFields() {
        var eventId = UUID.randomUUID();
        var processedAt = Instant.now();

        var event = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(processedAt)
                .build();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getProcessedAt()).isEqualTo(processedAt);
    }
}
