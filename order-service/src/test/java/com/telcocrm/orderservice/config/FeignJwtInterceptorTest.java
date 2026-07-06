package com.telcocrm.orderservice.config;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeignJwtInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private RequestTemplate template;

    private final FeignJwtInterceptor interceptor = new FeignJwtInterceptor();

    @Test
    void shouldForwardBearerAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        var attributes = new ServletRequestAttributes(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(template);
        }

        verify(template).header("Authorization", "Bearer token-123");
    }

    @Test
    void shouldNotForwardNonBearerAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Basic abcdef");
        var attributes = new ServletRequestAttributes(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(template);
        }

        verifyNoInteractions(template);
    }

    @Test
    void shouldDoNothingWhenNoRequestAttributes() {
        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            interceptor.apply(template);
        }

        verifyNoInteractions(template);
    }
}
