package com.telcocrm.notificationservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithCorrectMessage() {
        var ex = new ResourceNotFoundException("Notification", "id", "123");
        assertThat(ex.getMessage()).contains("Notification", "id", "123");
        assertThat(ex.getResourceName()).isEqualTo("Notification");
        assertThat(ex.getFieldName()).isEqualTo("id");
        assertThat(ex.getFieldValue()).isEqualTo("123");
    }
}
