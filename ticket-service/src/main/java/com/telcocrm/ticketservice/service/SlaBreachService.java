package com.telcocrm.ticketservice.service;

import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.event.publish.SlaBreachedEvent;
import com.telcocrm.ticketservice.event.publish.TicketEventTopics;
import com.telcocrm.ticketservice.repository.TicketRepository;
import com.telcocrm.ticketservice.rules.TicketStateRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaBreachService {

    private final TicketRepository ticketRepository;
    private final TicketStateRules ticketStateRules;
    private final OutboxService outboxService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UUID> findBreachedTicketIds(int batchSize) {
        return ticketRepository.findBreachedTickets(LocalDateTime.now(clock), PageRequest.of(0, batchSize))
                .stream()
                .map(Ticket::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishBreach(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.isSlaBreached()) {
            return;
        }

        ticketStateRules.markSlaBreached(ticket);
        ticketRepository.save(ticket);

        outboxService.saveEvent(
                TicketEventTopics.AGGREGATE_TYPE,
                ticket.getId().toString(),
                TicketEventTopics.SLA_BREACHED,
                SlaBreachedEvent.of(
                        ticket.getId(),
                        ticket.getCustomerId(),
                        ticket.getPriority(),
                        ticket.getAssignedTeam(),
                        ticket.getSlaDueAt()));

        log.info("SLA breached for ticket {} (team: {}, due: {})",
                ticket.getId(), ticket.getAssignedTeam(), ticket.getSlaDueAt());
    }
}
