package com.telcocrm.usageservice.dto.response;

import java.time.LocalDate;

public record AggregationRunResponse(
        LocalDate asOf,
        int aggregatedCount
) {}
