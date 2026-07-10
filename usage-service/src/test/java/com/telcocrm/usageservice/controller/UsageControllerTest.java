package com.telcocrm.usageservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.usageservice.dto.response.QuotaResponse;
import com.telcocrm.usageservice.dto.response.UsageRecordResponse;
import com.telcocrm.usageservice.entity.enums.UsageType;
import com.telcocrm.usageservice.exception.GlobalExceptionHandler;
import com.telcocrm.usageservice.exception.QuotaNotFoundException;
import com.telcocrm.usageservice.service.UsageQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsageQueryService usageQueryService;

    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(
                new SpringDataJacksonConfiguration.PageModule(
                        new SpringDataWebSettings(
                                EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));
    }

    @BeforeEach
    void setUp() {
        var controller = new UsageController(usageQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getQuota_shouldReturn200() throws Exception {
        var subscriptionId = UUID.randomUUID();
        var response = new QuotaResponse(subscriptionId, "905551112233", "TARIFF-100",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                1000, 200, 800, 500, 50, 450, 20000, 5000, 15000);
        when(usageQueryService.getQuota(subscriptionId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/usage/subscriptions/{subscriptionId}/quota", subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.minutesRemaining").value(800));
    }

    @Test
    void getQuota_shouldReturn404WhenNotFound() throws Exception {
        var subscriptionId = UUID.randomUUID();
        when(usageQueryService.getQuota(subscriptionId)).thenThrow(new QuotaNotFoundException(subscriptionId));

        mockMvc.perform(get("/api/v1/usage/subscriptions/{subscriptionId}/quota", subscriptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("QUOTA_NOT_FOUND"));
    }

    @Test
    void getHistory_shouldReturn200() throws Exception {
        var subscriptionId = UUID.randomUUID();
        var record = new UsageRecordResponse(UUID.randomUUID(), UsageType.DATA, 150,
                LocalDateTime.of(2026, 7, 15, 10, 0), "cdr-1");
        when(usageQueryService.getHistory(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(record)));

        mockMvc.perform(get("/api/v1/usage/subscriptions/{subscriptionId}/history", subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cdrRef").value("cdr-1"));
    }
}
