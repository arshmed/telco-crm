package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAlreadyAssignedExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var userId = UUID.randomUUID();

        var ex = new RoleAlreadyAssignedException(userId, "FIELD_DEALER");

        assertThat(ex.getMessage()).contains(userId.toString()).contains("FIELD_DEALER");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("ROLE_ALREADY_ASSIGNED");
    }
}
