package com.telcocrm.productcatalogservice.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
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
}
