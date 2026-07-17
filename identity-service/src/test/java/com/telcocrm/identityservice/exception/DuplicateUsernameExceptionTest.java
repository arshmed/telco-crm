package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateUsernameExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new DuplicateUsernameException("agokhan");

        assertThat(ex.getMessage()).contains("agokhan");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("DUPLICATE_USERNAME");
    }
}
