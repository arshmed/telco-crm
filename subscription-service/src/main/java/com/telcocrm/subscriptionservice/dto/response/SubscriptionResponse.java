package com.telcocrm.subscriptionservice.dto.response;

import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private UUID id;
    private UUID customerId;
    private String customerNo;
    private String msisdn;
    private String tariffCode;
    private SubscriptionStatus status;
    private LocalDateTime activatedAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime terminatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
