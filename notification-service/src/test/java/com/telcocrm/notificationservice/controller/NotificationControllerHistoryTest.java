package com.telcocrm.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.notificationservice.exception.GlobalExceptionHandler;
import com.telcocrm.notificationservice.exception.ResourceNotFoundException;
import com.telcocrm.notificationservice.security.CustomerAccessGuard;
import com.telcocrm.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerHistoryTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CustomerAccessGuard customerAccessGuard;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule(),
                    new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule(
                            new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.DIRECT)))
            .build();

    @BeforeEach
    void setUp() {
        var controller = new NotificationController(notificationService, customerAccessGuard);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200ForHistoryEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();

        when(notificationService.getUserNotificationHistory(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/notifications/users/{userId}/history", userId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenCustomerAccessGuardDenies() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Notification", "userId", userId))
                .when(customerAccessGuard).assertOwnResource(eq(userId), any());

        mockMvc.perform(get("/api/v1/notifications/users/{userId}/history", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WithRealCustomerAccessGuardWhenCustomerRequestsAnotherUsersHistory() throws Exception {
        UUID userId = UUID.randomUUID();
        var realGuardController = new NotificationController(notificationService, new CustomerAccessGuard());
        var realGuardMockMvc = MockMvcBuilders.standaloneSetup(realGuardController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("customer_id", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("CUSTOMER"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, authorities, "user-1"));
        try {
            realGuardMockMvc.perform(get("/api/v1/notifications/users/{userId}/history", userId))
                    .andExpect(status().isNotFound());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
