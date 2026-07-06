package com.telcocrm.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPersistenceExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageStatusAndCause() {
        var cause = new RuntimeException("serialization failed");

        var ex = new OutboxPersistenceException("order-created-topic", "order-123", cause);

        assertThat(ex.getMessage()).contains("order-created-topic").contains("order-123");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getErrorCode()).isEqualTo("OUTBOX_PERSISTENCE_FAILED");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
