package com.telcocrm.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRefundExceptionTest {

    @Test
    void shouldBuildExceptionWithUnprocessableEntityStatusAndErrorCode() {
        UUID paymentId = UUID.randomUUID();

        PaymentRefundException ex = new PaymentRefundException(paymentId, "Only completed payments can be refunded");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(ex.getErrorCode()).isEqualTo("PAYMENT_REFUND_FAILED");
        assertThat(ex.getMessage())
                .contains(paymentId.toString())
                .contains("Only completed payments can be refunded");
    }

    @Test
    void shouldIncludeNullReasonInMessageWithoutThrowing() {
        PaymentRefundException ex = new PaymentRefundException(UUID.randomUUID(), null);

        assertThat(ex.getMessage()).contains("null");
    }
}
