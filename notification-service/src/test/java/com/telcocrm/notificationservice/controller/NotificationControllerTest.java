package com.telcocrm.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.notificationservice.dto.NotificationRequest;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import com.telcocrm.notificationservice.enums.NotificationStatus;
import com.telcocrm.notificationservice.security.CustomerAccessGuard;
import com.telcocrm.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CustomerAccessGuard customerAccessGuard;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        var controller = new NotificationController(notificationService, customerAccessGuard);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldSendNotification() throws Exception {
        var request = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .templateCode("CUSTOMER_REGISTERED")
                .channel(NotificationChannel.EMAIL)
                .payload(Map.of("firstName", "John"))
                .build();

        var response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .templateCode(request.getTemplateCode())
                .channel(NotificationChannel.EMAIL)
                .body("Hello John!")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.sendNotification(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateCode").value("CUSTOMER_REGISTERED"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void shouldReturn204WhenOptedOut() throws Exception {
        var request = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .templateCode("CUSTOMER_REGISTERED")
                .channel(NotificationChannel.EMAIL)
                .build();

        when(notificationService.sendNotification(any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

}
