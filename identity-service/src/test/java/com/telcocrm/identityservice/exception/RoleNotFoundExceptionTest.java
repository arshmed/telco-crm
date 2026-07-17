package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new RoleNotFoundException("FIELD_DEALER");

        assertThat(ex.getMessage()).contains("FIELD_DEALER");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("ROLE_NOT_FOUND");
    }
}
