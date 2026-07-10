package com.telcocrm.usageservice.controller;

import com.telcocrm.usageservice.dto.response.AggregationRunResponse;
import com.telcocrm.usageservice.service.UsageAggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsageAggregationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsageAggregationService usageAggregationService;

    @Captor
    private ArgumentCaptor<LocalDate> asOfCaptor;

    @BeforeEach
    void setUp() {
        var controller = new UsageAggregationController(usageAggregationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void run_shouldUseProvidedAsOf() throws Exception {
        var asOf = LocalDate.of(2026, 7, 1);
        when(usageAggregationService.run(asOf)).thenReturn(new AggregationRunResponse(asOf, 3));

        mockMvc.perform(post("/api/v1/usage/aggregations/run").param("asOf", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregatedCount").value(3));
    }

    @Test
    void run_shouldDefaultToTodayWhenAsOfMissing() throws Exception {
        when(usageAggregationService.run(any())).thenReturn(new AggregationRunResponse(LocalDate.now(), 0));

        mockMvc.perform(post("/api/v1/usage/aggregations/run"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(usageAggregationService).run(asOfCaptor.capture());
        assertThat(asOfCaptor.getValue()).isEqualTo(LocalDate.now());
    }
}
