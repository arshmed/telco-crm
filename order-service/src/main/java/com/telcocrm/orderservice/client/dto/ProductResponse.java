package com.telcocrm.orderservice.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public record ProductResponse(
        String code,
        String name,
        @JsonAlias("monthlyFee")
        BigDecimal price,
        String status,
        String currency
) {}