package com.telcocrm.ticketservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketCommentResponse(
        UUID id,
        String authorId,
        String body,
        LocalDateTime createdAt
) {}
