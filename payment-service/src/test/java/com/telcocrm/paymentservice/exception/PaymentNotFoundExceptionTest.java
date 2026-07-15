package com.telcocrm.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithNotFoundStatusAndErrorCode() {
        UUID paymentId = UUID.randomUUID();

        PaymentNotFoundException ex = new PaymentNotFoundException(paymentId);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(ex.getMessage()).contains(paymentId.toString());
    }

    @Test
    void shouldIncludeNullIdInMessageWithoutThrowing() {
        PaymentNotFoundException ex = new PaymentNotFoundException(null);

        assertThat(ex.getMessage()).contains("null");
    }
}
