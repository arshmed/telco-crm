package com.telcocrm.ticketservice.rules;

import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.exception.TicketNotModifiableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TicketStateRules {

    private final Clock clock;

    public void assign(Ticket ticket, String assignedTeam) {
        requireNotResolved(ticket, "assigned");

        ticket.setAssignedTeam(assignedTeam);
        ticket.setStatus(TicketStatus.ASSIGNED);
    }

    public void resolve(Ticket ticket, String resolution) {
        requireNotResolved(ticket, "resolved");

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolution(resolution);
        ticket.setResolvedAt(LocalDateTime.now(clock));
    }

    public void markSlaBreached(Ticket ticket) {
        ticket.setSlaBreached(true);
    }

    private void requireNotResolved(Ticket ticket, String operation) {
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new TicketNotModifiableException(ticket.getId(), ticket.getStatus(), operation);
        }
    }
}
