package com.telcocrm.customerservice.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        var id = UUID.randomUUID();
        var ex = new ResourceNotFoundException("Customer", "id", id);
        assertThat(ex.getMessage()).contains("Customer not found").contains(id.toString());
    }

    @Test
    void shouldReturnFields() {
        var id = UUID.randomUUID();
        var ex = new ResourceNotFoundException("Customer", "id", id);
        assertThat(ex.getResourceName()).isEqualTo("Customer");
        assertThat(ex.getFieldName()).isEqualTo("id");
        assertThat(ex.getFieldValue()).isEqualTo(id);
    }
}
