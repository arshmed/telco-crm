package com.telcocrm.paymentservice.entity;

import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void addAttempt_shouldAddToListAndLinkBackToPayment() {
        Payment payment = buildPayment();
        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(1)
                .response("MOCK_PSP_APPROVED")
                .attemptedAt(Instant.now())
                .build();

        payment.addAttempt(attempt);

        assertThat(payment.getAttempts()).containsExactly(attempt);
        assertThat(attempt.getPayment()).isSameAs(payment);
    }

    @Test
    void removeAttempt_shouldRemoveFromListAndUnlinkPayment() {
        Payment payment = buildPayment();
        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(1)
                .response("MOCK_PSP_APPROVED")
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);

        payment.removeAttempt(attempt);

        assertThat(payment.getAttempts()).isEmpty();
        assertThat(attempt.getPayment()).isNull();
    }

    @Test
    void removeAttempt_shouldBeNoOpWhenAttemptNotPresent() {
        Payment payment = buildPayment();
        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(1)
                .response("MOCK_PSP_APPROVED")
                .attemptedAt(Instant.now())
                .build();

        payment.removeAttempt(attempt);

        assertThat(payment.getAttempts()).isEmpty();
        assertThat(attempt.getPayment()).isNull();
    }

    private Payment buildPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("10.00"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();
    }
}
