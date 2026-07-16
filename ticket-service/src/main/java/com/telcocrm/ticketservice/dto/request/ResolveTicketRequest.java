package com.telcocrm.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveTicketRequest(

        @NotBlank(message = "Resolution must not be blank")
        @Size(max = 1000, message = "Resolution must not exceed 1000 characters")
        String resolution
) {}
