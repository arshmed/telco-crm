package com.telcocrm.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAlreadyProcessedExceptionTest {

    @Test
    void shouldBuildExceptionWithConflictStatusAndErrorCode() {
        PaymentAlreadyProcessedException ex = new PaymentAlreadyProcessedException("req-77");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("PAYMENT_ALREADY_PROCESSED");
        assertThat(ex.getMessage()).contains("req-77");
    }

    @Test
    void shouldIncludeBlankPaymentRequestIdInMessage() {
        PaymentAlreadyProcessedException ex = new PaymentAlreadyProcessedException("");

        assertThat(ex.getMessage()).isNotBlank();
    }
}
