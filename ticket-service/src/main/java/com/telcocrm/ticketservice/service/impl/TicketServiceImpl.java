package com.telcocrm.ticketservice.service.impl;

import com.telcocrm.ticketservice.client.CustomerClient;
import com.telcocrm.ticketservice.client.dto.CustomerResponse;
import com.telcocrm.ticketservice.dto.request.AddCommentRequest;
import com.telcocrm.ticketservice.dto.request.AssignTicketRequest;
import com.telcocrm.ticketservice.dto.request.CreateTicketRequest;
import com.telcocrm.ticketservice.dto.request.ResolveTicketRequest;
import com.telcocrm.ticketservice.dto.response.TicketCommentResponse;
import com.telcocrm.ticketservice.dto.response.TicketResponse;
import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.entity.TicketComment;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.event.publish.TicketEventTopics;
import com.telcocrm.ticketservice.event.publish.TicketOpenedEvent;
import com.telcocrm.ticketservice.event.publish.TicketResolvedEvent;
import com.telcocrm.ticketservice.exception.TicketNotFoundException;
import com.telcocrm.ticketservice.mapper.TicketCommentMapper;
import com.telcocrm.ticketservice.mapper.TicketMapper;
import com.telcocrm.ticketservice.repository.TicketCommentRepository;
import com.telcocrm.ticketservice.repository.TicketRepository;
import com.telcocrm.ticketservice.rules.TicketSlaRules;
import com.telcocrm.ticketservice.rules.TicketStateRules;
import com.telcocrm.ticketservice.service.OutboxService;
import com.telcocrm.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final CustomerClient customerClient;
    private final TicketMapper ticketMapper;
    private final TicketCommentMapper ticketCommentMapper;
    private final TicketSlaRules ticketSlaRules;
    private final TicketStateRules ticketStateRules;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        CustomerResponse customer = customerClient.getCustomerById(request.customerId());
        if (!ACTIVE_STATUS.equals(customer.status())) {
            throw new IllegalStateException("Customer " + request.customerId() + " is not active");
        }

        String assignedTeam = ticketSlaRules.resolveTeam(request.category());
        LocalDateTime slaDueAt = ticketSlaRules.calculateSlaDueAt(request.priority());

        Ticket ticket = Ticket.builder()
                .customerId(request.customerId())
                .category(request.category())
                .priority(request.priority())
                .status(TicketStatus.ASSIGNED)
                .description(request.description())
                .assignedTeam(assignedTeam)
                .slaDueAt(slaDueAt)
                .build();

        ticketRepository.save(ticket);

        outboxService.saveEvent(
                TicketEventTopics.AGGREGATE_TYPE,
                ticket.getId().toString(),
                TicketEventTopics.TICKET_OPENED,
                TicketOpenedEvent.of(
                        ticket.getId(),
                        ticket.getCustomerId(),
                        ticket.getCategory(),
                        ticket.getPriority(),
                        ticket.getAssignedTeam(),
                        ticket.getSlaDueAt(),
                        customer.email(),
                        customer.firstName(),
                        customer.lastName()));

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID ticketId) {
        return ticketMapper.toResponse(requireTicket(ticketId));
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(UUID ticketId, AddCommentRequest request, String authorId) {
        Ticket ticket = requireTicket(ticketId);

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(authorId)
                .body(request.body())
                .build();

        ticketCommentRepository.save(comment);

        return ticketCommentMapper.toResponse(comment);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request) {
        Ticket ticket = requireTicket(ticketId);

        ticketStateRules.assign(ticket, request.assignedTeam());
        ticketRepository.save(ticket);

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse resolveTicket(UUID ticketId, ResolveTicketRequest request) {
        Ticket ticket = requireTicket(ticketId);

        ticketStateRules.resolve(ticket, request.resolution());
        ticketRepository.save(ticket);

        CustomerResponse customer = customerClient.getCustomerById(ticket.getCustomerId());

        outboxService.saveEvent(
                TicketEventTopics.AGGREGATE_TYPE,
                ticket.getId().toString(),
                TicketEventTopics.TICKET_RESOLVED,
                TicketResolvedEvent.of(
                        ticket.getId(),
                        ticket.getCustomerId(),
                        ticket.getResolution(),
                        ticket.getResolvedAt(),
                        customer.email(),
                        customer.firstName(),
                        customer.lastName()));

        return ticketMapper.toResponse(ticket);
    }

    private Ticket requireTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }
}
