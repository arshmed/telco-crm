package com.telcocrm.ticketservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(

        @NotBlank(message = "Body must not be blank")
        @Size(max = 2000, message = "Body must not exceed 2000 characters")
        String body
) {}
