package com.telcocrm.ticketservice.event.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResolvedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID ticketId,
        UUID customerId,
        String resolution,
        LocalDateTime resolvedAt,
        String email,
        String firstName,
        String lastName
) {
    public static TicketResolvedEvent of(UUID ticketId, UUID customerId, String resolution,
                                         LocalDateTime resolvedAt, String email, String firstName, String lastName) {
        return new TicketResolvedEvent(UUID.randomUUID(), LocalDateTime.now(), ticketId, customerId,
                resolution, resolvedAt, email, firstName, lastName);
    }
}
