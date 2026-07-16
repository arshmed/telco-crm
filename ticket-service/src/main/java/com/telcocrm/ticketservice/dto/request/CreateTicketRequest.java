package com.telcocrm.ticketservice.dto.request;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(

        @NotNull(message = "Customer ID must not be null")
        UUID customerId,

        @NotNull(message = "Category must not be null")
        TicketCategory category,

        @NotNull(message = "Priority must not be null")
        TicketPriority priority,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {}
