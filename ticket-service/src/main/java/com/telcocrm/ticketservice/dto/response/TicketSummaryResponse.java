package com.telcocrm.ticketservice.dto.response;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        UUID customerId,
        String customerName,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String description,
        String assignedTeam,
        LocalDateTime slaDueAt,
        LocalDateTime createdAt
) {}
