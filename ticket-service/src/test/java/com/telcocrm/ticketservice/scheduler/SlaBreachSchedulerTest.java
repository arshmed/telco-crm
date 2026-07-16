package com.telcocrm.ticketservice.scheduler;

import com.telcocrm.ticketservice.service.SlaBreachService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaBreachSchedulerTest {

    @Mock
    private SlaBreachService slaBreachService;

    private SlaBreachScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SlaBreachScheduler(slaBreachService);
        ReflectionTestUtils.setField(scheduler, "batchSize", 100);
    }

    @Test
    void shouldPublishBreachForEachCandidate() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(slaBreachService.findBreachedTicketIds(100)).thenReturn(List.of(first, second));

        scheduler.scanForBreaches();

        verify(slaBreachService).publishBreach(first);
        verify(slaBreachService).publishBreach(second);
    }

    @Test
    void shouldDoNothingWhenNoBreachedTickets() {
        when(slaBreachService.findBreachedTicketIds(100)).thenReturn(List.of());

        scheduler.scanForBreaches();

        verify(slaBreachService, never()).publishBreach(any());
    }

    @Test
    void shouldContinueWhenAnotherInstanceAlreadyBreachedTheTicket() {
        UUID contended = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        when(slaBreachService.findBreachedTicketIds(100)).thenReturn(List.of(contended, healthy));
        doThrow(new ObjectOptimisticLockingFailureException("tickets", contended))
                .when(slaBreachService).publishBreach(contended);

        scheduler.scanForBreaches();

        verify(slaBreachService).publishBreach(healthy);
    }

    @Test
    void shouldContinueWhenOneTicketFailsUnexpectedly() {
        UUID failing = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        when(slaBreachService.findBreachedTicketIds(100)).thenReturn(List.of(failing, healthy));
        doThrow(new RuntimeException("outbox down")).when(slaBreachService).publishBreach(failing);

        scheduler.scanForBreaches();

        verify(slaBreachService).publishBreach(healthy);
    }
}
