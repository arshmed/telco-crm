package com.telcocrm.customerservice.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CustomerKYCApprovedEvent(
    UUID customerId,
    String firstName,
    String lastName,
    String email,
    LocalDateTime approvedAt
) {
    public static CustomerKYCApprovedEvent of(UUID customerId, String firstName, String lastName, String email) {
        return CustomerKYCApprovedEvent.builder()
            .customerId(customerId)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .approvedAt(LocalDateTime.now())
            .build();
    }
}
