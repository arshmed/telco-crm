package com.telcocrm.paymentservice.exception;

import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidPaymentStateExceptionTest {

    @Test
    void shouldBuildExceptionWithConflictStatusAndStateDetails() {
        UUID paymentId = UUID.randomUUID();

        InvalidPaymentStateException ex = new InvalidPaymentStateException(
                paymentId, PaymentStatus.COMPLETED, PaymentStatus.PENDING);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_PAYMENT_STATE");
        assertThat(ex.getMessage())
                .contains(paymentId.toString())
                .contains("Current: COMPLETED")
                .contains("Expected: PENDING");
    }

    @Test
    void shouldHandleNullExpectedStatusGracefully() {
        UUID paymentId = UUID.randomUUID();

        InvalidPaymentStateException ex = new InvalidPaymentStateException(paymentId, PaymentStatus.FAILED, null);

        assertThat(ex.getMessage()).contains("Expected: null");
    }
}
