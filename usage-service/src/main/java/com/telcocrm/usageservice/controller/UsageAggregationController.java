package com.telcocrm.usageservice.controller;

import com.telcocrm.usageservice.dto.response.AggregationRunResponse;
import com.telcocrm.usageservice.service.UsageAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/usage/aggregations")
@RequiredArgsConstructor
public class UsageAggregationController {

    private final UsageAggregationService usageAggregationService;

    @PostMapping("/run")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'SYSTEM_SERVICE')")
    public AggregationRunResponse run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return usageAggregationService.run(asOf != null ? asOf : LocalDate.now());
    }
}
