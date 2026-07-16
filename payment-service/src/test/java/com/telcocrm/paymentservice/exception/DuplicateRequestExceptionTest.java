package com.telcocrm.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateRequestExceptionTest {

    @Test
    void shouldBuildExceptionWithConflictStatusAndErrorCode() {
        DuplicateRequestException ex = new DuplicateRequestException("req-123");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_REQUEST");
        assertThat(ex.getMessage()).contains("req-123");
    }

    @Test
    void shouldIncludeNullPaymentRequestIdInMessageWithoutThrowing() {
        DuplicateRequestException ex = new DuplicateRequestException(null);

        assertThat(ex.getMessage()).contains("null");
    }
}
