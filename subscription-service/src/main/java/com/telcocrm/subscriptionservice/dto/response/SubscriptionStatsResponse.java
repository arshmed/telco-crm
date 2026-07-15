package com.telcocrm.subscriptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionStatsResponse {
    private long active;
    private long suspended;
    private long pending;
    private long terminated;
    private long total;
}
