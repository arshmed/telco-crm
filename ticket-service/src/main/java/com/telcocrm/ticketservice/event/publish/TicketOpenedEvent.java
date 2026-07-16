package com.telcocrm.ticketservice.event.publish;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketOpenedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID ticketId,
        UUID customerId,
        TicketCategory category,
        TicketPriority priority,
        String assignedTeam,
        LocalDateTime slaDueAt,
        String email,
        String firstName,
        String lastName
) {
    public static TicketOpenedEvent of(UUID ticketId, UUID customerId, TicketCategory category,
                                       TicketPriority priority, String assignedTeam, LocalDateTime slaDueAt,
                                       String email, String firstName, String lastName) {
        return new TicketOpenedEvent(UUID.randomUUID(), LocalDateTime.now(), ticketId, customerId, category,
                priority, assignedTeam, slaDueAt, email, firstName, lastName);
    }
}
