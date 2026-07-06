package com.telcocrm.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceUnavailableExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageStatusAndCause() {
        var cause = new RuntimeException("connection refused");

        var ex = new ServiceUnavailableException("Customer service", "CUSTOMER_SERVICE_UNAVAILABLE", cause);

        assertThat(ex.getMessage()).contains("Customer service");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOMER_SERVICE_UNAVAILABLE");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
