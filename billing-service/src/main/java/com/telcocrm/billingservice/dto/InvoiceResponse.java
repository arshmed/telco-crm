package com.telcocrm.billingservice.dto;

import com.telcocrm.billingservice.enums.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private UUID id;
    private UUID customerId;
    private String customerNo;
    private UUID subscriptionId;
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal subTotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
    private List<InvoiceLineResponse> lines;
    private LocalDateTime createdAt;
}
