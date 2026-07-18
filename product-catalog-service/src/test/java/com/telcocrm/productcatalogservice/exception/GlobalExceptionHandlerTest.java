package com.telcocrm.productcatalogservice.exception;

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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TariffNotFoundException("GNC-20GB"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertEquals("TARIFF_NOT_FOUND", body.getTitle());
        assertEquals("TARIFF_NOT_FOUND", body.getProperties().get("errorCode"));
    }

    @Test
    void problemDetailIncludesCorrelationIdFromMdc() {
        MDC.put("correlationId", "abc-123");

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TariffNotFoundException("GNC-20GB"));

        assertEquals("abc-123", response.getBody().getProperties().get("correlationId"));
    }

    @Test
    void problemDetailOmitsCorrelationIdWhenAbsent() {
        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TariffNotFoundException("GNC-20GB"));

        assertFalse(response.getBody().getProperties().containsKey("correlationId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validationExceptionCollectsFieldErrors() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "tariff");
        binding.addError(new FieldError("tariff", "code", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, List<String>> errors =
                (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertEquals(List.of("must not be blank"), errors.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void constraintViolationExceptionCollectsMessages() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("price");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        List<String> errors = (List<String>) response.getBody().getProperties().get("errors");
        assertTrue(errors.contains("price: must be positive"));
    }

    @Test
    void typeMismatchExceptionDescribesParameter() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "version", null, null);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatchException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_PARAMETER", response.getBody().getTitle());
        assertTrue(response.getBody().getDetail().contains("version"));
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
    void authorizationDeniedExceptionReturns403() {
        ResponseEntity<ProblemDetail> response =
                handler.handleAuthorizationDeniedException(new AuthorizationDeniedException("Access Denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCESS_DENIED", response.getBody().getTitle());
        assertEquals("ACCESS_DENIED", response.getBody().getProperties().get("errorCode"));
    }

    @Test
    void genericExceptionReturns500() {
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getTitle());
    }
}
