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
        UUID invoiceId = UUID.randomUUID();

        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, paymentId, invoiceId);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.invoiceId()).isEqualTo(invoiceId);
    }

    @Test
    void of_shouldAllowNullInvoiceId() {
        PaymentCompletedEvent event = PaymentCompletedEvent.of(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(event.invoiceId()).isNull();
    }

    @Test
    void of_shouldGenerateDifferentEventIdsForEachCall() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCompletedEvent first = PaymentCompletedEvent.of(orderId, paymentId, null);
        PaymentCompletedEvent second = PaymentCompletedEvent.of(orderId, paymentId, null);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }
}
