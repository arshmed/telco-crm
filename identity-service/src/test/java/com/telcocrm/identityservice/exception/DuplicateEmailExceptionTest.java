package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateEmailExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new DuplicateEmailException("agokhan@example.com");

        assertThat(ex.getMessage()).contains("agokhan@example.com");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_EMAIL");
    }
}
