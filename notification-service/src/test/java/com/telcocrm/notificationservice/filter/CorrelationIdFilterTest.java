package com.telcocrm.notificationservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void shouldUseExistingCorrelationId() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("existing-id");

        var filter = new CorrelationIdFilter();
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Correlation-Id", "existing-id");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        var filter = new CorrelationIdFilter();
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetMdc() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("mdc-test");

        var filter = new CorrelationIdFilter();
        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo("mdc-test");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);
    }
}
