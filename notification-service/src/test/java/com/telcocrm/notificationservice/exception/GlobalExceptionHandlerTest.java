package com.telcocrm.notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void shouldHandleResourceNotFound() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications/test");

        var ex = new ResourceNotFoundException("Notification", "id", UUID_STRING);
        ProblemDetail result = handler.handleResourceNotFound(ex, request);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getTitle()).isEqualTo("Resource Not Found");
    }

    @Test
    void shouldHandleValidationErrors() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "templateCode", "must not be blank")));

        ProblemDetail result = handler.handleValidationErrors(validationException, request);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getTitle()).isEqualTo("Validation Failed");
        assertThat(result.getDetail()).contains("must not be blank");
    }

    @Test
    void shouldHandleIllegalArgument() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");

        var ex = new IllegalArgumentException("Invalid channel");
        ProblemDetail result = handler.handleIllegalArgument(ex, request);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getDetail()).isEqualTo("Invalid channel");
    }

    @Test
    void shouldHandleGeneralException() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");

        var ex = new RuntimeException("Unexpected");
        ProblemDetail result = handler.handleGeneral(ex, request);

        assertThat(result.getStatus()).isEqualTo(500);
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
    }

    @Test
    void shouldAddCorrelationIdWhenPresent() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-correlation-id");

        var ex = new ResourceNotFoundException("Notification", "id", UUID_STRING);
        ProblemDetail result = handler.handleResourceNotFound(ex, request);

        assertThat(result.getProperties()).containsEntry("correlationId", "test-correlation-id");
    }

    private static final String UUID_STRING = "550e8400-e29b-41d4-a716-446655440000";
}
