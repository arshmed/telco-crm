package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new PermissionNotFoundException("USER_WRITE");

        assertThat(ex.getMessage()).contains("USER_WRITE");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("PERMISSION_NOT_FOUND");
    }
}
