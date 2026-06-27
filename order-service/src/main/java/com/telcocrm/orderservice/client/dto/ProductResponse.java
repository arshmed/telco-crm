package com.telcocrm.orderservice.client.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String code,
        String name,
        BigDecimal price,
        String status 
) {}