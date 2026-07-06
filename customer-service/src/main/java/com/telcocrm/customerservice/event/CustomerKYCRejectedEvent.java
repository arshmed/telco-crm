package com.telcocrm.customerservice.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CustomerKYCRejectedEvent(
    UUID customerId,
    String firstName,
    String lastName,
    LocalDateTime rejectedAt
) {
    public static CustomerKYCRejectedEvent of(UUID customerId, String firstName, String lastName) {
        return CustomerKYCRejectedEvent.builder()
            .customerId(customerId)
            .firstName(firstName)
            .lastName(lastName)
            .rejectedAt(LocalDateTime.now())
            .build();
    }
}
