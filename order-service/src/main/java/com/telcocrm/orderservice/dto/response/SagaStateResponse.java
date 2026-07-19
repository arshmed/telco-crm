package com.telcocrm.orderservice.dto.response;

import com.telcocrm.orderservice.entity.enums.SagaStep;

import java.time.LocalDateTime;

public record SagaStateResponse(
        SagaStep currentStep,
        int retryCount,
        String errorMessage,
        LocalDateTime lastUpdated
) {}
