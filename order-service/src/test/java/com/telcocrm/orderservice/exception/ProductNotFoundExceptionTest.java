package com.telcocrm.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNotFoundExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new ProductNotFoundException("TARIFF-99");

        assertThat(ex.getMessage()).contains("TARIFF-99");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("PRODUCT_NOT_FOUND");
    }
}
