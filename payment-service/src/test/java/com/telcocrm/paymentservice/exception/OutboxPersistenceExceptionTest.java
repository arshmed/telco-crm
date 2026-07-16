package com.telcocrm.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPersistenceExceptionTest {

    @Test
    void shouldBuildExceptionWithInternalServerErrorAndCause() {
        RuntimeException cause = new RuntimeException("db unreachable");

        OutboxPersistenceException ex = new OutboxPersistenceException("payment-completed-topic", "payment-1", cause);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getErrorCode()).isEqualTo("OUTBOX_PERSISTENCE_FAILED");
        assertThat(ex.getMessage()).contains("payment-completed-topic").contains("payment-1");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void shouldAllowNullCause() {
        OutboxPersistenceException ex = new OutboxPersistenceException("topic", "id", null);

        assertThat(ex.getCause()).isNull();
    }
}
