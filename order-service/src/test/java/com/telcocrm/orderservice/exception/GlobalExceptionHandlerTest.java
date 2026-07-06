package com.telcocrm.orderservice.exception;

import feign.FeignException;
import feign.Request;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBaseException() {
        var orderId = UUID.randomUUID();
        var ex = new OrderNotFoundException(orderId);

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "ORDER_NOT_FOUND");
        assertThat(response.getBody().getDetail()).contains(orderId.toString());
    }

    @Test
    void shouldHandleValidationException() {
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "customerId", "Customer ID must not be null"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("VALIDATION_ERROR");
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, java.util.List<String>>) response.getBody().getProperties().get("errors");
        assertThat(errors.get("customerId")).contains("Customer ID must not be null");
    }

    @Test
    void shouldHandleConstraintViolationException() {
        var ex = new ConstraintViolationException("invalid", Set.of());

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void shouldHandleTypeMismatchException() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("orderId");
        when(ex.getValue()).thenReturn("not-a-uuid");

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatchException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetail()).contains("orderId").contains("not-a-uuid");
    }

    @Test
    void shouldHandleMessageNotReadableException() {
        var ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ProblemDetail> response = handler.handleMessageNotReadableException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void shouldHandleOptimisticLockingFailureException() {
        var ex = new ObjectOptimisticLockingFailureException("Order", UUID.randomUUID());

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailureException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("CONCURRENT_UPDATE");
    }

    @Test
    void shouldHandleIllegalStateException() {
        var ex = new IllegalStateException("Customer is not active");

        ResponseEntity<ProblemDetail> response = handler.handleIllegalStateException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("ILLEGAL_STATE");
        assertThat(response.getBody().getDetail()).isEqualTo("Customer is not active");
    }

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/customers/1", Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void shouldHandleFeignNotFoundAs404() {
        var ex = new FeignException.NotFound("not found", dummyRequest(), null, null);

        ResponseEntity<ProblemDetail> response = handler.handleFeignException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("UPSTREAM_SERVICE_ERROR");
    }

    @Test
    void shouldHandleOtherFeignExceptionAs503() {
        FeignException ex = new FeignException(500, "server error") {};

        ResponseEntity<ProblemDetail> response = handler.handleFeignException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldHandleGenericException() {
        var ex = new RuntimeException("boom");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getDetail()).isEqualTo("Unexpected error occurred");
    }
}
