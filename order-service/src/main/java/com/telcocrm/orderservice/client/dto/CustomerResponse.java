package com.telcocrm.orderservice.client.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String status 
) {}