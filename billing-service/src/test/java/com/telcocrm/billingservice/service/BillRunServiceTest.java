package com.telcocrm.billingservice.service;

import com.telcocrm.billingservice.client.ProductCatalogClient;
import com.telcocrm.billingservice.entity.BillCycle;
import com.telcocrm.billingservice.entity.Invoice;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import com.telcocrm.billingservice.exception.OutboxPersistenceException;
import com.telcocrm.billingservice.repository.BillCycleRepository;
import com.telcocrm.billingservice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillRunServiceTest {

    @Mock
    private BillCycleRepository billCycleRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ProductCatalogClient productCatalogClient;
    @Mock
    private OutboxService outboxService;

    private BillRunService billRunService;

    private UUID customerId;
    private UUID subscriptionId;
    private BillCycle billCycle;

    private UUID savedInvoiceId;

    @BeforeEach
    void setUp() {
        billRunService = new BillRunService(billCycleRepository, invoiceRepository, productCatalogClient, outboxService);
        customerId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        savedInvoiceId = UUID.randomUUID();
        billCycle = BillCycle.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .subscriptionId(subscriptionId)
                .tariffCode("TARIFF-001")
                .dayOfMonth(1)
                .nextRunDate(LocalDate.now().minusDays(1))
                .active(true)
                .build();
    }

    @Test
    void runBillCycle_noDueCycles_returnsZero() {
        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isZero();
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void runBillCycle_oneDueCycle_generatesInvoice() {
        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(false);
        when(invoiceRepository.count()).thenReturn(0L);
        when(productCatalogClient.getTariff("TARIFF-001"))
                .thenReturn(Map.of("code", "TARIFF-001", "monthlyFee", new BigDecimal("99.90")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(savedInvoiceId);
            return invoice;
        });

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isOne();
        verify(invoiceRepository).save(any(Invoice.class));
        verify(outboxService).saveEvent(eq("INVOICE"), anyString(), eq("invoice-generated-topic"), any());
        verify(billCycleRepository, atLeastOnce()).save(billCycle);
    }

    @Test
    void runBillCycle_invoiceAlreadyExists_skipsGeneration() {
        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(true);

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isOne();
        verify(invoiceRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(anyString(), anyString(), anyString(), any());
        verify(billCycleRepository, atLeastOnce()).save(billCycle);
    }

    @Test
    void runBillCycle_multipleCycles_generatesAll() {
        BillCycle cycle2 = BillCycle.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .tariffCode("TARIFF-002")
                .dayOfMonth(1)
                .nextRunDate(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle, cycle2));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(false);
        when(invoiceRepository.count()).thenReturn(0L);
        when(productCatalogClient.getTariff(anyString()))
                .thenReturn(Map.of("monthlyFee", new BigDecimal("50.00")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(savedInvoiceId);
            return invoice;
        });

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isEqualTo(2);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    void runBillCycle_generateInvoiceFailsContinuesWithNext() {
        BillCycle cycle2 = BillCycle.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .tariffCode("TARIFF-002")
                .dayOfMonth(1)
                .nextRunDate(LocalDate.now().minusDays(1))
                .active(true)
                .build();

        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle, cycle2));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(false);
        when(productCatalogClient.getTariff("TARIFF-001"))
                .thenThrow(new RuntimeException("Feign error"));
        when(productCatalogClient.getTariff("TARIFF-002"))
                .thenReturn(Map.of("monthlyFee", new BigDecimal("50.00")));
        when(invoiceRepository.count()).thenReturn(0L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(savedInvoiceId);
            return invoice;
        });

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isOne();
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void runBillCycle_invoiceGeneratedHasCorrectFields() {
        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(false);
        when(invoiceRepository.count()).thenReturn(5L);
        when(productCatalogClient.getTariff("TARIFF-001"))
                .thenReturn(Map.of("monthlyFee", new BigDecimal("100.00")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(savedInvoiceId);
            return invoice;
        });

        billRunService.runBillCycle(LocalDate.now());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(saved.getSubTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(saved.getTaxRate()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(saved.getTaxAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(saved.getGrandTotal()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(saved.getLines()).hasSize(1);
        assertThat(saved.getLines().get(0).getDescription()).contains("TARIFF-001");
        assertThat(saved.getInvoiceNumber()).startsWith("INV-");
    }

    @Test
    void createBillCycle_savesWithCorrectFields() {
        billRunService.createBillCycle(customerId, subscriptionId, "TARIFF-001", 5);

        ArgumentCaptor<BillCycle> captor = ArgumentCaptor.forClass(BillCycle.class);
        verify(billCycleRepository).save(captor.capture());
        BillCycle saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getTariffCode()).isEqualTo("TARIFF-001");
        assertThat(saved.getDayOfMonth()).isEqualTo(5);
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getNextRunDate()).isNotNull();
    }

    @Test
    void createBillCycle_clampsDayToMonthLength() {
        billRunService.createBillCycle(customerId, subscriptionId, "TARIFF-001", 31);

        ArgumentCaptor<BillCycle> captor = ArgumentCaptor.forClass(BillCycle.class);
        verify(billCycleRepository).save(captor.capture());

        BillCycle saved = captor.getValue();
        assertThat(saved.getNextRunDate().getDayOfMonth())
                .isLessThanOrEqualTo(YearMonth.now().lengthOfMonth());
    }

    @Test
    void runBillCycle_outboxFailsInvoiceStillSaved() {
        when(billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(any(LocalDate.class)))
                .thenReturn(List.of(billCycle));
        when(invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(
                any(), any(), any()))
                .thenReturn(false);
        when(invoiceRepository.count()).thenReturn(0L);
        when(productCatalogClient.getTariff("TARIFF-001"))
                .thenReturn(Map.of("monthlyFee", new BigDecimal("99.90")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(savedInvoiceId);
            return invoice;
        });
        doThrow(new OutboxPersistenceException("topic", "aggId", new RuntimeException()))
                .when(outboxService).saveEvent(anyString(), anyString(), anyString(), any());

        int result = billRunService.runBillCycle(LocalDate.now());

        assertThat(result).isZero();
        verify(invoiceRepository).save(any(Invoice.class));
    }
}
