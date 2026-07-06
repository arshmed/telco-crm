package com.telcocrm.customerservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateResourceExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        var ex = new DuplicateResourceException("Customer", "identityNumber", "12345");
        assertThat(ex.getMessage()).contains("Customer already exists").contains("12345");
    }

    @Test
    void shouldReturnFields() {
        var ex = new DuplicateResourceException("Customer", "identityNumber", "12345");
        assertThat(ex.getResourceName()).isEqualTo("Customer");
        assertThat(ex.getFieldName()).isEqualTo("identityNumber");
        assertThat(ex.getFieldValue()).isEqualTo("12345");
    }
}
