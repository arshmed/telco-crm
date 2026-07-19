package com.telcocrm.usageservice.client.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String status,
        String email,
        String firstName,
        String lastName
) {}
