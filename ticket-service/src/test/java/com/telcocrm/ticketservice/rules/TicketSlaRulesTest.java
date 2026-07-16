package com.telcocrm.ticketservice.rules;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TicketSlaRulesTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T10:00:00Z");

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
    private final TicketSlaRules slaRules = new TicketSlaRules(fixedClock);

    @ParameterizedTest
    @CsvSource({
            "COMPLAINT, complaint-team",
            "REQUEST, request-team",
            "FAULT, fault-team"
    })
    void shouldResolveTeamForEachCategory(TicketCategory category, String expectedTeam) {
        assertThat(slaRules.resolveTeam(category)).isEqualTo(expectedTeam);
    }

    @ParameterizedTest
    @CsvSource({
            "URGENT, 4",
            "HIGH, 8",
            "MEDIUM, 24",
            "LOW, 72"
    })
    void shouldReturnSlaHoursForEachPriority(TicketPriority priority, int expectedHours) {
        assertThat(slaRules.slaHours(priority)).isEqualTo(expectedHours);
    }

    @ParameterizedTest
    @EnumSource(TicketPriority.class)
    void shouldMapEveryPriorityToSlaHours(TicketPriority priority) {
        assertThat(slaRules.slaHours(priority)).isPositive();
    }

    @Test
    void shouldCalculateSlaDueAtFromClock() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        assertThat(slaRules.calculateSlaDueAt(TicketPriority.URGENT)).isEqualTo(now.plusHours(4));
        assertThat(slaRules.calculateSlaDueAt(TicketPriority.LOW)).isEqualTo(now.plusHours(72));
    }

    @Test
    void shouldGiveTighterSlaToHigherPriority() {
        assertThat(slaRules.slaHours(TicketPriority.URGENT))
                .isLessThan(slaRules.slaHours(TicketPriority.HIGH));
        assertThat(slaRules.slaHours(TicketPriority.HIGH))
                .isLessThan(slaRules.slaHours(TicketPriority.MEDIUM));
        assertThat(slaRules.slaHours(TicketPriority.MEDIUM))
                .isLessThan(slaRules.slaHours(TicketPriority.LOW));
    }
}
