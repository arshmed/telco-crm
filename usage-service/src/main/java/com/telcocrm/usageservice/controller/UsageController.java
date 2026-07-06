package com.telcocrm.usageservice.controller;

import com.telcocrm.usageservice.dto.response.QuotaResponse;
import com.telcocrm.usageservice.dto.response.UsageRecordResponse;
import com.telcocrm.usageservice.service.UsageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage/subscriptions/{subscriptionId}")
@RequiredArgsConstructor
public class UsageController {

    private final UsageQueryService usageQueryService;

    @GetMapping("/quota")
    public QuotaResponse getQuota(@PathVariable UUID subscriptionId) {
        return usageQueryService.getQuota(subscriptionId);
    }

    @GetMapping("/history")
    public Page<UsageRecordResponse> getHistory(
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return usageQueryService.getHistory(subscriptionId, from, to, pageable);
    }
}
