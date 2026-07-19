package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPersistenceExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var cause = new RuntimeException("db down");

        var ex = new OutboxPersistenceException("user-created-topic", "user-123", cause);

        assertThat(ex.getMessage()).contains("user-created-topic").contains("user-123");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getErrorCode()).isEqualTo("OUTBOX_PERSISTENCE_FAILED");
    }
}
