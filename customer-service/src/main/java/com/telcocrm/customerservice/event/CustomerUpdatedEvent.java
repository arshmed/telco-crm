package com.telcocrm.customerservice.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CustomerUpdatedEvent(
    UUID customerId,
    String firstName,
    String lastName,
    String email,
    String phone,
    LocalDateTime updatedAt
) {
    public static CustomerUpdatedEvent of(UUID customerId, String firstName, String lastName,
                                          String email, String phone) {
        return CustomerUpdatedEvent.builder()
            .customerId(customerId)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .phone(phone)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
