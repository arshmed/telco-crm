package com.telcocrm.paymentservice.event.publish;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRefundedEventTest {

    @Test
    void of_shouldPopulateEventIdAndOccurredAtAndCarryFields() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("149.90");

        PaymentRefundedEvent event = PaymentRefundedEvent.of(orderId, paymentId, amount);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.refundedAmount()).isEqualByComparingTo(amount);
    }

    @Test
    void of_shouldAllowNullRefundedAmount() {
        PaymentRefundedEvent event = PaymentRefundedEvent.of(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(event.refundedAmount()).isNull();
    }
}
