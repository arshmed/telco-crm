package com.telcocrm.paymentservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(
        UUID id,
        Integer attemptNo,
        String response,
        Instant attemptedAt
) {}
