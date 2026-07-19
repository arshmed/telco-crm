package com.telcocrm.customerservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
    }

    @Test
    void shouldHandleResourceNotFound() {
        var ex = new ResourceNotFoundException("Customer", "id", UUID.randomUUID());
        ProblemDetail problem = handler.handleResourceNotFound(ex, request);
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getType()).isEqualTo(URI.create("https://telco.example/errors/resource-not-found"));
    }

    @Test
    void shouldHandleDuplicateResource() {
        var ex = new DuplicateResourceException("Customer", "identityNumber", "12345678901");
        ProblemDetail problem = handler.handleDuplicateResource(ex, request);
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Duplicate Resource");
    }

    @Test
    void shouldHandleValidationErrors() {
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "firstName", "First name is required"));
        bindingResult.addError(new FieldError("target", "lastName", "Last name is required"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidationErrors(ex, request);
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).contains("First name is required");
        assertThat(problem.getDetail()).contains("Last name is required");
    }

    @Test
    void shouldHandleIllegalArgument() {
        var ex = new IllegalArgumentException("Invalid status transition");
        ProblemDetail problem = handler.handleIllegalArgument(ex, request);
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Invalid status transition");
    }

    @Test
    void shouldHandleGeneralException() {
        var ex = new RuntimeException("Unexpected error");
        ProblemDetail problem = handler.handleGeneral(ex, request);
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void shouldIncludeCorrelationIdWhenPresent() {
        when(request.getHeader("X-Correlation-Id")).thenReturn("corr-123");
        var ex = new ResourceNotFoundException("Customer", "id", UUID.randomUUID());
        ProblemDetail problem = handler.handleResourceNotFound(ex, request);
        assertThat(problem.getProperties()).isNotNull();
        assertThat(problem.getProperties()).containsEntry("correlationId", "corr-123");
    }

    @Test
    void shouldNotIncludeCorrelationIdWhenAbsent() {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        var ex = new ResourceNotFoundException("Customer", "id", UUID.randomUUID());
        ProblemDetail problem = handler.handleResourceNotFound(ex, request);
        assertThat(problem.getProperties()).isNull();
    }
}
