package com.telcocrm.orderservice.entity;

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
                .aggregateType("ORDER")
                .aggregateId("order-123")
                .topic("order-created-topic")
                .payload("{}")
                .createdAt(createdAt)
                .build();

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getAggregateType()).isEqualTo("ORDER");
        assertThat(event.getAggregateId()).isEqualTo("order-123");
        assertThat(event.getTopic()).isEqualTo("order-created-topic");
        assertThat(event.getPayload()).isEqualTo("{}");
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
    }
}
