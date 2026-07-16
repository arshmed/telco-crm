package com.telcocrm.ticketservice.repository;

import com.telcocrm.ticketservice.entity.Ticket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.slaBreached = false
              AND t.slaDueAt < :now
              AND t.status <> com.telcocrm.ticketservice.entity.enums.TicketStatus.RESOLVED
            ORDER BY t.slaDueAt ASC
            """)
    List<Ticket> findBreachedTickets(@Param("now") LocalDateTime now, Pageable pageable);
}
