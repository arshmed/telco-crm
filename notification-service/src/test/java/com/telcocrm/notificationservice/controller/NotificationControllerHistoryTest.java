package com.telcocrm.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.notificationservice.exception.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerHistoryTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule(),
                    new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule(
                            new SpringDataWebSettings(EnableSpringDataWebSupport.PageSerializationMode.DIRECT)))
            .build();

    @BeforeEach
    void setUp() {
        var controller = new NotificationController(notificationService);
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
}
