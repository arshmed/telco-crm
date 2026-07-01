package com.telcocrm.customerservice.event;

import com.telcocrm.customerservice.enums.CustomerType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CustomerRegisteredEvent(
    UUID customerId,
    CustomerType type,
    String firstName,
    String lastName,
    String identityNumber,
    LocalDateTime registeredAt
) {
    public static CustomerRegisteredEvent of(UUID customerId, CustomerType type,
                                             String firstName, String lastName,
                                             String identityNumber) {
        return CustomerRegisteredEvent.builder()
            .customerId(customerId)
            .type(type)
            .firstName(firstName)
            .lastName(lastName)
            .identityNumber(identityNumber)
            .registeredAt(LocalDateTime.now())
            .build();
    }
}
