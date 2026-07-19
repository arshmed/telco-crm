package com.telcocrm.orderservice.config;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeignJwtInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private RequestTemplate template;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    private FeignJwtInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new FeignJwtInterceptor(authorizedClientManager);
    }

    @Test
    void shouldForwardBearerAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        var attributes = new ServletRequestAttributes(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(template);
        }

        verify(template).header("Authorization", "Bearer token-123");
        verifyNoInteractions(authorizedClientManager);
    }

    @Test
    void shouldFallBackToClientCredentialsWhenNoBearerAuthorizationHeader() {
        when(request.getHeader("Authorization")).thenReturn("Basic abcdef");
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("service-account-token");
        var attributes = new ServletRequestAttributes(request);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(template);
        }

        verify(template).header("Authorization", "Bearer service-account-token");
    }

    @Test
    void shouldFallBackToClientCredentialsWhenNoRequestAttributes() {
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("service-account-token");

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            interceptor.apply(template);
        }

        verify(template).header("Authorization", "Bearer service-account-token");
    }

    @Test
    void shouldNotSetHeaderWhenClientCredentialsAuthorizationFails() {
        when(authorizedClientManager.authorize(any())).thenReturn(null);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            interceptor.apply(template);
        }

        verifyNoInteractions(template);
    }
}
