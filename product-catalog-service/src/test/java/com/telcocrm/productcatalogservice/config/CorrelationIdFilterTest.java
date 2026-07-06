package com.telcocrm.productcatalogservice.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void usesIncomingHeaderAndExposesItToMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Correlation-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenByChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenByChain.set(MDC.get("correlationId")));

        assertEquals("abc-123", seenByChain.get());
        assertEquals("abc-123", response.getHeader("Correlation-Id"));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void generatesIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenByChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenByChain.set(MDC.get("correlationId")));

        assertNotNull(seenByChain.get());
        assertEquals(seenByChain.get(), response.getHeader("Correlation-Id"));
    }
}
