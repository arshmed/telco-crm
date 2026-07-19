package com.telcocrm.orderservice.exception;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNotCancellableExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var orderId = UUID.randomUUID();

        var ex = new OrderNotCancellableException(orderId, OrderStatus.FULFILLED);

        assertThat(ex.getMessage()).contains(orderId.toString()).contains("FULFILLED");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(ex.getErrorCode()).isEqualTo("ORDER_NOT_CANCELLABLE");
    }
}
