package com.telcocrm.ticketservice.event.publish;

import com.telcocrm.ticketservice.entity.enums.TicketPriority;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlaBreachedEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        UUID ticketId,
        UUID customerId,
        TicketPriority priority,
        String assignedTeam,
        LocalDateTime slaDueAt
) {
    public static SlaBreachedEvent of(UUID ticketId, UUID customerId, TicketPriority priority,
                                      String assignedTeam, LocalDateTime slaDueAt) {
        return new SlaBreachedEvent(UUID.randomUUID(), LocalDateTime.now(), ticketId, customerId,
                priority, assignedTeam, slaDueAt);
    }
}
