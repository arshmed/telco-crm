package com.telcocrm.subscriptionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.telcocrm.subscriptionservice.dto.response.SubscriptionResponse;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import com.telcocrm.subscriptionservice.exception.GlobalExceptionHandler;
import com.telcocrm.subscriptionservice.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubscriptionService subscriptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SubscriptionController controller = new SubscriptionController(subscriptionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createSubscription_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(id)
                .customerId(customerId)
                .msisdn("5551234567")
                .tariffCode("TARIFE-001")
                .status(SubscriptionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(subscriptionService.createSubscription(any())).thenReturn(response);

        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .customerId(customerId)
                .tariffCode("TARIFE-001")
                .msisdn("5551234567")
                .build();

        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getSubscription_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .msisdn("5551234567")
                .tariffCode("TARIFE-001")
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(subscriptionService.getSubscription(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/subscriptions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void activateSubscription_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(id)
                .status(SubscriptionStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        when(subscriptionService.activateSubscription(id)).thenReturn(response);

        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void suspendSubscription_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(id)
                .status(SubscriptionStatus.SUSPENDED)
                .suspendedAt(LocalDateTime.now())
                .build();

        when(subscriptionService.suspendSubscription(id)).thenReturn(response);

        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void terminateSubscription_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(id)
                .status(SubscriptionStatus.TERMINATED)
                .terminatedAt(LocalDateTime.now())
                .build();

        when(subscriptionService.terminateSubscription(id)).thenReturn(response);

        mockMvc.perform(post("/api/v1/subscriptions/" + id + "/terminate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }
}
