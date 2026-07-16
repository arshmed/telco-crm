package com.telcocrm.billingservice.service;

import com.telcocrm.billingservice.dto.InvoiceResponse;
import com.telcocrm.billingservice.entity.Invoice;
import com.telcocrm.billingservice.entity.InvoiceLine;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import com.telcocrm.billingservice.exception.ResourceNotFoundException;
import com.telcocrm.billingservice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID customerId;
    private UUID invoiceId;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        InvoiceLine line = InvoiceLine.builder()
                .id(UUID.randomUUID())
                .description("Aylik Tarife Ucreti - TARIFF-001")
                .quantity(1)
                .unitPrice(new BigDecimal("99.90"))
                .lineTotal(new BigDecimal("99.90"))
                .build();

        invoice = Invoice.builder()
                .id(invoiceId)
                .customerId(customerId)
                .subscriptionId(UUID.randomUUID())
                .invoiceNumber("INV-202607-0001")
                .periodStart(java.time.LocalDate.of(2026, 6, 1))
                .periodEnd(java.time.LocalDate.of(2026, 6, 30))
                .subTotal(new BigDecimal("99.90"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("19.98"))
                .grandTotal(new BigDecimal("119.88"))
                .status(InvoiceStatus.ISSUED)
                .dueDate(java.time.LocalDate.of(2026, 7, 14))
                .issuedAt(LocalDateTime.now())
                .lines(new java.util.ArrayList<>(List.of(line)))
                .build();
    }

    @Test
    void listInvoices_returnsPagedResult() {
        when(invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(invoice));

        Pageable pageable = PageRequest.of(0, 20);
        Page<InvoiceResponse> result = invoiceService.listInvoices(customerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-202607-0001");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void listInvoices_emptyResult() {
        when(invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of());

        Page<InvoiceResponse> result = invoiceService.listInvoices(customerId, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void listInvoices_includesLines() {
        when(invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(invoice));

        Page<InvoiceResponse> result = invoiceService.listInvoices(customerId, PageRequest.of(0, 20));

        assertThat(result.getContent().get(0).getLines()).hasSize(1);
        assertThat(result.getContent().get(0).getLines().get(0).getDescription())
                .isEqualTo("Aylik Tarife Ucreti - TARIFF-001");
    }

    @Test
    void getInvoice_found() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        InvoiceResponse result = invoiceService.getInvoice(invoiceId);

        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getGrandTotal()).isEqualByComparingTo(new BigDecimal("119.88"));
    }

    @Test
    void getInvoice_notFound_throws() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoice(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice");
    }

    @Test
    void markInvoicePaid_setsStatusAndPaidAt() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.markInvoicePaid(invoiceId);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(saved.getPaidAt()).isNotNull();
    }

    @Test
    void markInvoicePaid_alreadyPaid_skips() {
        invoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        invoiceService.markInvoicePaid(invoiceId);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markInvoicePaid_notFound_throws() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.markInvoicePaid(invoiceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice");
    }

    @Test
    void listInvoices_multipleInvoices_orderedByCreatedAtDesc() {
        Invoice older = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .invoiceNumber("INV-202606-0001")
                .status(InvoiceStatus.PAID)
                .lines(new java.util.ArrayList<>())
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
        Invoice newer = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .invoiceNumber("INV-202607-0002")
                .status(InvoiceStatus.ISSUED)
                .lines(new java.util.ArrayList<>())
                .createdAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                .build();

        when(invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(newer, older));

        Page<InvoiceResponse> result = invoiceService.listInvoices(customerId, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-202607-0002");
        assertThat(result.getContent().get(1).getInvoiceNumber()).isEqualTo("INV-202606-0001");
    }
}
