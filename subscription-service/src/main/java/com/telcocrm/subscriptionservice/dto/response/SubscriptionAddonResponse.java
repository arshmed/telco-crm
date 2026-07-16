package com.telcocrm.subscriptionservice.dto.response;

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
public class SubscriptionAddonResponse {

    private UUID id;
    private String addonCode;
    private LocalDateTime addedAt;
}
