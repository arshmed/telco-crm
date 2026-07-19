package com.telcocrm.orderservice.dto.response;

import com.telcocrm.orderservice.entity.enums.OrderItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String productCode,
        String productName,
        OrderItemType productType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}