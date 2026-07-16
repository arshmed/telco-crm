package com.telcocrm.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTicketRequest(

        @NotBlank(message = "Assigned team must not be blank")
        @Size(max = 100, message = "Assigned team must not exceed 100 characters")
        String assignedTeam
) {}
