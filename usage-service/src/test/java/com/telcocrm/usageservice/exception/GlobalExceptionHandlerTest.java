package com.telcocrm.usageservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void baseExceptionMapsStatusTitleAndErrorCode() {
        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new QuotaNotFoundException(UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("QUOTA_NOT_FOUND", response.getBody().getTitle());
        assertEquals("QUOTA_NOT_FOUND", response.getBody().getProperties().get("errorCode"));
    }

    @Test
    void baseExceptionIncludesCorrelationIdFromMdc() {
        MDC.put("correlationId", "abc-123");

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TariffNotFoundException("GNC-20GB"));

        assertEquals("abc-123", response.getBody().getProperties().get("correlationId"));
    }

    @Test
    void baseExceptionOmitsCorrelationIdWhenAbsent() {
        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TariffNotFoundException("GNC-20GB"));

        assertFalse(response.getBody().getProperties().containsKey("correlationId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validationExceptionCollectsFieldErrors() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "quota");
        binding.addError(new FieldError("quota", "subscriptionId", "must not be null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, List<String>> errors =
                (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertEquals(List.of("must not be null"), errors.get("subscriptionId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void constraintViolationExceptionCollectsMessages() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("from");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        List<String> errors = (List<String>) response.getBody().getProperties().get("errors");
        assertTrue(errors.contains("from: must not be null"));
    }

    @Test
    void typeMismatchExceptionDescribesParameter() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", UUID.class, "subscriptionId", null, null);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatchException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PARAMETER", response.getBody().getTitle());
        assertTrue(response.getBody().getDetail().contains("subscriptionId"));
    }

    @Test
    void messageNotReadableExceptionReturnsBadRequest() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ProblemDetail> response = handler.handleMessageNotReadableException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MALFORMED_REQUEST", response.getBody().getTitle());
    }

    @Test
    void accessDeniedExceptionReturnsForbidden() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCESS_DENIED", response.getBody().getTitle());
    }

    @Test
    void genericExceptionReturns500() {
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getTitle());
    }
}
