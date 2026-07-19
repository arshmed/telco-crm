package com.telcocrm.ticketservice.rules;

import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.exception.TicketNotModifiableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStateRulesTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T10:00:00Z");

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
    private final TicketStateRules stateRules = new TicketStateRules(fixedClock);

    private Ticket ticketWithStatus(TicketStatus status) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .status(status)
                .assignedTeam("fault-team")
                .build();
    }

    @Test
    void shouldReassignOpenTicketToAnotherTeam() {
        var ticket = ticketWithStatus(TicketStatus.ASSIGNED);

        stateRules.assign(ticket, "complaint-team");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(ticket.getAssignedTeam()).isEqualTo("complaint-team");
    }

    @Test
    void shouldRejectAssignOnResolvedTicket() {
        var ticket = ticketWithStatus(TicketStatus.RESOLVED);

        assertThatThrownBy(() -> stateRules.assign(ticket, "complaint-team"))
                .isInstanceOf(TicketNotModifiableException.class)
                .hasMessageContaining("assigned");

        assertThat(ticket.getAssignedTeam()).isEqualTo("fault-team");
    }

    @Test
    void shouldResolveAssignedTicket() {
        var ticket = ticketWithStatus(TicketStatus.ASSIGNED);

        stateRules.resolve(ticket, "modem yeniden başlatıldı");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolution()).isEqualTo("modem yeniden başlatıldı");
        assertThat(ticket.getResolvedAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    void shouldRejectResolvingAnAlreadyResolvedTicket() {
        var ticket = ticketWithStatus(TicketStatus.RESOLVED);

        assertThatThrownBy(() -> stateRules.resolve(ticket, "tekrar"))
                .isInstanceOf(TicketNotModifiableException.class)
                .hasMessageContaining("resolved");

        assertThat(ticket.getResolution()).isNull();
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @Test
    void shouldMarkSlaBreached() {
        var ticket = ticketWithStatus(TicketStatus.ASSIGNED);
        assertThat(ticket.isSlaBreached()).isFalse();

        stateRules.markSlaBreached(ticket);

        assertThat(ticket.isSlaBreached()).isTrue();
    }
}
