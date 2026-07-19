package com.telcocrm.usageservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionsTest {

    @Test
    void quotaNotFoundCarriesNotFoundStatus() {
        QuotaNotFoundException ex = new QuotaNotFoundException(UUID.randomUUID());

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("QUOTA_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void tariffNotFoundCarriesNotFoundStatus() {
        TariffNotFoundException ex = new TariffNotFoundException("GNC-20GB");

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("TARIFF_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void serviceUnavailableCarriesStatusErrorCodeAndCause() {
        RuntimeException cause = new RuntimeException("down");
        ServiceUnavailableException ex = new ServiceUnavailableException("customer-service", "CUSTOMER_UNAVAILABLE", cause);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("CUSTOMER_UNAVAILABLE", ex.getErrorCode());
        assertSame(cause, ex.getCause());
    }
}
