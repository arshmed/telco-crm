package com.telcocrm.ticketservice.service;

import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.event.publish.SlaBreachedEvent;
import com.telcocrm.ticketservice.event.publish.TicketEventTopics;
import com.telcocrm.ticketservice.repository.TicketRepository;
import com.telcocrm.ticketservice.rules.TicketStateRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaBreachServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T10:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private OutboxService outboxService;

    private SlaBreachService slaBreachService;

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
    private final UUID ticketId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        slaBreachService = new SlaBreachService(
                ticketRepository, new TicketStateRules(fixedClock), outboxService, fixedClock);
    }

    private Ticket breachedTicket() {
        return Ticket.builder()
                .id(ticketId)
                .customerId(customerId)
                .category(TicketCategory.FAULT)
                .priority(TicketPriority.URGENT)
                .status(TicketStatus.ASSIGNED)
                .description("İki gündür sinyal yok")
                .assignedTeam("fault-team")
                .slaDueAt(LocalDateTime.now(fixedClock).minusHours(1))
                .slaBreached(false)
                .build();
    }

    @Test
    void shouldFindBreachedTicketIdsUsingClockTime() {
        when(ticketRepository.findBreachedTickets(eq(LocalDateTime.now(fixedClock)), any(Pageable.class)))
                .thenReturn(List.of(breachedTicket()));

        List<UUID> ids = slaBreachService.findBreachedTicketIds(50);

        assertThat(ids).containsExactly(ticketId);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketRepository).findBreachedTickets(eq(LocalDateTime.now(fixedClock)), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 50));
    }

    @Test
    void shouldPublishBreachEventAndSetFlag() {
        Ticket ticket = breachedTicket();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        slaBreachService.publishBreach(ticketId);

        assertThat(ticket.isSlaBreached()).isTrue();
        verify(ticketRepository).save(ticket);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).saveEvent(eq(TicketEventTopics.AGGREGATE_TYPE), eq(ticketId.toString()),
                eq(TicketEventTopics.SLA_BREACHED), captor.capture());

        SlaBreachedEvent event = (SlaBreachedEvent) captor.getValue();
        assertThat(event.ticketId()).isEqualTo(ticketId);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.priority()).isEqualTo(TicketPriority.URGENT);
        assertThat(event.assignedTeam()).isEqualTo("fault-team");
    }

    @Test
    void shouldNotPublishTwiceForAlreadyBreachedTicket() {
        Ticket ticket = breachedTicket();
        ticket.setSlaBreached(true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        slaBreachService.publishBreach(ticketId);

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldSkipSilentlyWhenTicketDisappeared() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        slaBreachService.publishBreach(ticketId);

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }
}
