package com.telcocrm.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var customerId = UUID.randomUUID();

        var ex = new CustomerNotFoundException(customerId);

        assertThat(ex.getMessage()).contains(customerId.toString());
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOMER_NOT_FOUND");
    }
}
