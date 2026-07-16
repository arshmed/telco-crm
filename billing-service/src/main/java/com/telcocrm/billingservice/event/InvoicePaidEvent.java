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
public class InvoicePaidEvent {

    private UUID eventId;
    private Instant occurredAt;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID customerId;
    private BigDecimal grandTotal;
    private String email;
    private String firstName;
    private String lastName;

    public static InvoicePaidEvent of(UUID invoiceId, String invoiceNumber, UUID customerId,
                                      BigDecimal grandTotal, String email, String firstName, String lastName) {
        return InvoicePaidEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceNumber)
                .customerId(customerId)
                .grandTotal(grandTotal)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
