package com.telcocrm.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class InvoiceStatsResponse {
    private long overdueCount;
    private long paidCount;
    private long issuedCount;
    private long totalCount;
    private BigDecimal totalRevenue;
}
