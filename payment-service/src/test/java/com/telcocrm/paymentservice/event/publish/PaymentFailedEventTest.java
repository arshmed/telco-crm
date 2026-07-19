package com.telcocrm.paymentservice.event.publish;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFailedEventTest {

    @Test
    void of_shouldPopulateEventIdAndOccurredAtAndCarryFields() {
        UUID orderId = UUID.randomUUID();

        PaymentFailedEvent event = PaymentFailedEvent.of(orderId, "Card declined");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.reason()).isEqualTo("Card declined");
    }

    @Test
    void of_shouldAllowNullReason() {
        PaymentFailedEvent event = PaymentFailedEvent.of(UUID.randomUUID(), null);

        assertThat(event.reason()).isNull();
    }
}
