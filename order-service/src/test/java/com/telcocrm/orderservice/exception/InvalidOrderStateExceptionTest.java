package com.telcocrm.orderservice.exception;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidOrderStateExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var orderId = UUID.randomUUID();

        var ex = new InvalidOrderStateException(orderId, OrderStatus.PAID, OrderStatus.PENDING_PAYMENT);

        assertThat(ex.getMessage())
                .contains(orderId.toString())
                .contains("PAID")
                .contains("PENDING_PAYMENT");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_ORDER_STATE");
    }
}
