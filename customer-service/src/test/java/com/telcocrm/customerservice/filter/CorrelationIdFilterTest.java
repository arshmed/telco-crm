package com.telcocrm.customerservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Captor
    private ArgumentCaptor<String> correlationIdCaptor;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldUseExistingCorrelationId() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("existing-id-123");
        doAnswer(inv -> {
            assertThat(MDC.get("correlationId")).isEqualTo("existing-id-123");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), eq("existing-id-123"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        doAnswer(inv -> {
            String cid = MDC.get("correlationId");
            assertThat(cid).matches("[a-f0-9\\-]{36}");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), correlationIdCaptor.capture());
        assertThat(correlationIdCaptor.getValue()).matches("[a-f0-9\\-]{36}");
    }

    @Test
    void shouldGenerateCorrelationIdWhenBlank() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), correlationIdCaptor.capture());
        assertThat(correlationIdCaptor.getValue()).matches("[a-f0-9\\-]{36}");
    }

    @Test
    void shouldProceedWithFilterChain() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldCleanupMdcAfterRequest() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("test-id");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("correlationId")).isNull();
    }
}
