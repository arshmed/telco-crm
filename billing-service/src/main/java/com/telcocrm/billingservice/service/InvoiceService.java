package com.telcocrm.billingservice.service;

import com.telcocrm.billingservice.dto.InvoiceLineResponse;
import com.telcocrm.billingservice.dto.InvoiceResponse;
import com.telcocrm.billingservice.entity.Invoice;
import com.telcocrm.billingservice.entity.InvoiceLine;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import com.telcocrm.billingservice.event.InvoiceGeneratedEvent;
import com.telcocrm.billingservice.event.InvoicePaidEvent;
import com.telcocrm.billingservice.exception.ResourceNotFoundException;
import com.telcocrm.billingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(UUID customerId, Pageable pageable) {
        List<Invoice> invoices = customerId == null
                ? invoiceRepository.findAllByOrderByCreatedAtDesc()
                : invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return invoices.stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return toResponse(invoice);
    }

    @Transactional
    public void markInvoicePaid(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.info("Invoice {} already paid, skipping", invoiceId);
            return;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("Invoice {} marked as PAID", invoice.getInvoiceNumber());
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineResponse> lines = invoice.getLines() != null
                ? invoice.getLines().stream().map(line -> InvoiceLineResponse.builder()
                        .id(line.getId())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .lineTotal(line.getLineTotal())
                        .build()).toList()
                : List.of();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .customerId(invoice.getCustomerId())
                .subscriptionId(invoice.getSubscriptionId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .subTotal(invoice.getSubTotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .grandTotal(invoice.getGrandTotal())
                .status(invoice.getStatus())
                .dueDate(invoice.getDueDate())
                .issuedAt(invoice.getIssuedAt())
                .paidAt(invoice.getPaidAt())
                .lines(lines)
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
