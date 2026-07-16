package com.telcocrm.ticketservice.dto.response;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID customerId,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String description,
        String assignedTeam,
        LocalDateTime slaDueAt,
        String resolution,
        LocalDateTime resolvedAt,
        List<TicketCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
