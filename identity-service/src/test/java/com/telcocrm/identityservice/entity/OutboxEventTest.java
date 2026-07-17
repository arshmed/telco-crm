package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var createdAt = Instant.now();

        var event = OutboxEvent.builder()
                .id(id)
                .aggregateType("USER")
                .aggregateId("user-123")
                .topic("user-created-topic")
                .payload("{}")
                .createdAt(createdAt)
                .build();

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getAggregateType()).isEqualTo("USER");
        assertThat(event.getAggregateId()).isEqualTo("user-123");
        assertThat(event.getTopic()).isEqualTo("user-created-topic");
        assertThat(event.getPayload()).isEqualTo("{}");
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }
}
