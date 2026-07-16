package com.telcocrm.ticketservice.scheduler;

import com.telcocrm.ticketservice.service.SlaBreachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaBreachScheduler {

    private final SlaBreachService slaBreachService;

    @Value("${ticket.sla.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${ticket.sla.scan-interval-ms:60000}")
    public void scanForBreaches() {
        List<UUID> breachedIds = slaBreachService.findBreachedTicketIds(batchSize);
        if (breachedIds.isEmpty()) {
            return;
        }

        int published = 0;
        for (UUID ticketId : breachedIds) {
            try {
                slaBreachService.publishBreach(ticketId);
                published++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("Ticket {} already breached by another instance, skipping", ticketId);
            } catch (Exception e) {
                log.error("Failed to publish SLA breach for ticket {}", ticketId, e);
            }
        }

        log.info("SLA breach scan published {} of {} candidate tickets", published, breachedIds.size());
    }
}
