package com.telcocrm.paymentservice.event.publish;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCompletedEventTest {

    @Test
    void of_shouldPopulateEventIdAndOccurredAtAndCarryFields() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, paymentId);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.paymentId()).isEqualTo(paymentId);
    }

    @Test
    void of_shouldGenerateDifferentEventIdsForEachCall() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCompletedEvent first = PaymentCompletedEvent.of(orderId, paymentId);
        PaymentCompletedEvent second = PaymentCompletedEvent.of(orderId, paymentId);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }
}
