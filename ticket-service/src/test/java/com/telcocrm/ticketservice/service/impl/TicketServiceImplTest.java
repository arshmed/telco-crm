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
import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.event.publish.TicketEventTopics;
import com.telcocrm.ticketservice.event.publish.TicketOpenedEvent;
import com.telcocrm.ticketservice.event.publish.TicketResolvedEvent;
import com.telcocrm.ticketservice.exception.TicketNotFoundException;
import com.telcocrm.ticketservice.exception.TicketNotModifiableException;
import com.telcocrm.ticketservice.mapper.TicketCommentMapper;
import com.telcocrm.ticketservice.mapper.TicketMapper;
import com.telcocrm.ticketservice.repository.TicketCommentRepository;
import com.telcocrm.ticketservice.repository.TicketRepository;
import com.telcocrm.ticketservice.rules.TicketSlaRules;
import com.telcocrm.ticketservice.rules.TicketStateRules;
import com.telcocrm.ticketservice.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T10:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketCommentMapper ticketCommentMapper;
    @Mock
    private OutboxService outboxService;

    private TicketServiceImpl ticketService;

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final UUID customerId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository,
                ticketCommentRepository,
                customerClient,
                ticketMapper,
                ticketCommentMapper,
                new TicketSlaRules(fixedClock),
                new TicketStateRules(fixedClock),
                outboxService);
    }

    private CustomerResponse activeCustomer() {
        return new CustomerResponse(customerId, "ACTIVE", "ayse@example.com", "Ayşe", "Yılmaz");
    }

    private void stubSaveAssigningId() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(ticketId);
            }
            return toSave;
        });
    }

    private CreateTicketRequest createRequest(TicketCategory category, TicketPriority priority) {
        return new CreateTicketRequest(customerId, category, priority, "Geçen ay fazla ücret alındı");
    }

    private Ticket existingTicket(TicketStatus status) {
        return Ticket.builder()
                .id(ticketId)
                .customerId(customerId)
                .category(TicketCategory.COMPLAINT)
                .priority(TicketPriority.HIGH)
                .status(status)
                .description("Geçen ay fazla ücret alındı")
                .assignedTeam("complaint-team")
                .slaDueAt(LocalDateTime.now(fixedClock).plusHours(8))
                .build();
    }

    // ---- createTicket ----

    @Test
    void shouldCreateTicketWithAutoAssignedTeamAndSlaDueDate() {
        when(customerClient.getCustomerById(customerId)).thenReturn(activeCustomer());
        stubSaveAssigningId();

        ticketService.createTicket(createRequest(TicketCategory.FAULT, TicketPriority.URGENT));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(saved.getAssignedTeam()).isEqualTo("fault-team");
        assertThat(saved.getSlaDueAt()).isEqualTo(LocalDateTime.now(fixedClock).plusHours(4));
        assertThat(saved.isSlaBreached()).isFalse();
    }

    @Test
    void shouldPublishExactlyOneOpenedEventOnCreate() {
        when(customerClient.getCustomerById(customerId)).thenReturn(activeCustomer());
        stubSaveAssigningId();

        ticketService.createTicket(createRequest(TicketCategory.COMPLAINT, TicketPriority.MEDIUM));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).saveEvent(eq(TicketEventTopics.AGGREGATE_TYPE), anyString(),
                eq(TicketEventTopics.TICKET_OPENED), eventCaptor.capture());
        verifyNoMoreOutboxEvents();

        TicketOpenedEvent opened = (TicketOpenedEvent) eventCaptor.getValue();
        assertThat(opened.customerId()).isEqualTo(customerId);
        assertThat(opened.category()).isEqualTo(TicketCategory.COMPLAINT);
        assertThat(opened.assignedTeam()).isEqualTo("complaint-team");
        assertThat(opened.email()).isEqualTo("ayse@example.com");
        assertThat(opened.firstName()).isEqualTo("Ayşe");
        assertThat(opened.eventId()).isNotNull();
    }

    @Test
    void shouldRejectCreateWhenCustomerIsNotActive() {
        when(customerClient.getCustomerById(customerId))
                .thenReturn(new CustomerResponse(customerId, "PENDING", "a@b.com", "A", "B"));

        assertThatThrownBy(() -> ticketService.createTicket(createRequest(TicketCategory.REQUEST, TicketPriority.LOW)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not active");

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    // ---- getTicketById ----

    @Test
    void shouldReturnTicketById() {
        Ticket ticket = existingTicket(TicketStatus.ASSIGNED);
        TicketResponse expected = sampleResponse();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        assertThat(ticketService.getTicketById(ticketId)).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenTicketNotFound() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ---- addComment ----

    @Test
    void shouldAddCommentWithAuthorFromCaller() {
        Ticket ticket = existingTicket(TicketStatus.ASSIGNED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketCommentMapper.toResponse(any()))
                .thenReturn(new TicketCommentResponse(UUID.randomUUID(), "agent-7", "bakıyorum", null));

        TicketCommentResponse response =
                ticketService.addComment(ticketId, new AddCommentRequest("bakıyorum"), "agent-7");

        ArgumentCaptor<TicketComment> captor = ArgumentCaptor.forClass(TicketComment.class);
        verify(ticketCommentRepository).save(captor.capture());
        TicketComment saved = captor.getValue();

        assertThat(saved.getAuthorId()).isEqualTo("agent-7");
        assertThat(saved.getBody()).isEqualTo("bakıyorum");
        assertThat(saved.getTicket()).isEqualTo(ticket);
        assertThat(response).isNotNull();
    }

    // ---- assignTicket ----

    @Test
    void shouldReassignTicketWithoutPublishingEvent() {
        Ticket ticket = existingTicket(TicketStatus.ASSIGNED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.assignTicket(ticketId, new AssignTicketRequest("fault-team"));

        assertThat(ticket.getAssignedTeam()).isEqualTo("fault-team");
        verify(ticketRepository).save(ticket);
        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldRejectAssignWhenTicketIsResolved() {
        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(existingTicket(TicketStatus.RESOLVED)));

        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, new AssignTicketRequest("fault-team")))
                .isInstanceOf(TicketNotModifiableException.class);
    }

    // ---- resolveTicket ----

    @Test
    void shouldResolveTicketAndPublishEventWithCustomerContact() {
        Ticket ticket = existingTicket(TicketStatus.ASSIGNED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(customerClient.getCustomerById(customerId)).thenReturn(activeCustomer());

        ticketService.resolveTicket(ticketId, new ResolveTicketRequest("Fatura düzeltildi"));

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolution()).isEqualTo("Fatura düzeltildi");
        assertThat(ticket.getResolvedAt()).isEqualTo(LocalDateTime.now(fixedClock));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).saveEvent(eq(TicketEventTopics.AGGREGATE_TYPE), eq(ticketId.toString()),
                eq(TicketEventTopics.TICKET_RESOLVED), captor.capture());

        TicketResolvedEvent event = (TicketResolvedEvent) captor.getValue();
        assertThat(event.resolution()).isEqualTo("Fatura düzeltildi");
        assertThat(event.email()).isEqualTo("ayse@example.com");
        assertThat(event.resolvedAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    void shouldRejectResolvingAnAlreadyResolvedTicket() {
        when(ticketRepository.findById(ticketId))
                .thenReturn(Optional.of(existingTicket(TicketStatus.RESOLVED)));

        assertThatThrownBy(() -> ticketService.resolveTicket(ticketId, new ResolveTicketRequest("tekrar")))
                .isInstanceOf(TicketNotModifiableException.class);

        verifyNoInteractions(outboxService);
        verifyNoInteractions(customerClient);
    }

    private void verifyNoMoreOutboxEvents() {
        verify(outboxService, org.mockito.Mockito.times(1)).saveEvent(anyString(), anyString(), anyString(), any());
    }

    private TicketResponse sampleResponse() {
        return new TicketResponse(ticketId, customerId, TicketCategory.COMPLAINT, TicketPriority.HIGH,
                TicketStatus.ASSIGNED, "Geçen ay fazla ücret alındı", "complaint-team",
                LocalDateTime.now(fixedClock).plusHours(8), null, null, List.of(),
                LocalDateTime.now(fixedClock), LocalDateTime.now(fixedClock));
    }
}
