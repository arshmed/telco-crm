package com.telcocrm.billingservice.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceGeneratedEvent {

    private UUID eventId;
    private Instant occurredAt;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID customerId;
    private UUID subscriptionId;
    private BigDecimal grandTotal;
    private String email;
    private String firstName;
    private String lastName;

    public static InvoiceGeneratedEvent of(UUID invoiceId, String invoiceNumber, UUID customerId,
                                           UUID subscriptionId, BigDecimal grandTotal,
                                           String email, String firstName, String lastName) {
        return InvoiceGeneratedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .customerId(customerId)
                .subscriptionId(subscriptionId)
                .grandTotal(grandTotal)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
