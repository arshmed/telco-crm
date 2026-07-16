package com.telcocrm.ticketservice.client.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String status,
        String email,
        String firstName,
        String lastName
) {}
