package com.telcocrm.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var orderId = UUID.randomUUID();

        var ex = new OrderNotFoundException(orderId);

        assertThat(ex.getMessage()).contains(orderId.toString());
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("ORDER_NOT_FOUND");
    }
}
