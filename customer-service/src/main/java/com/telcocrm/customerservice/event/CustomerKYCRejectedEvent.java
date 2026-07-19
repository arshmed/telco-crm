package com.telcocrm.customerservice.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CustomerKYCRejectedEvent(
    UUID customerId,
    String firstName,
    String lastName,
    String email,
    LocalDateTime rejectedAt
) {
    public static CustomerKYCRejectedEvent of(UUID customerId, String firstName, String lastName, String email) {
        return CustomerKYCRejectedEvent.builder()
            .customerId(customerId)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .rejectedAt(LocalDateTime.now())
            .build();
    }
}
