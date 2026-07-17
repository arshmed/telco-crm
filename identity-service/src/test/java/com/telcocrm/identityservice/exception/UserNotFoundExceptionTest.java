package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var userId = UUID.randomUUID();

        var ex = new UserNotFoundException(userId);

        assertThat(ex.getMessage()).contains(userId.toString());
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("USER_NOT_FOUND");
    }
}
