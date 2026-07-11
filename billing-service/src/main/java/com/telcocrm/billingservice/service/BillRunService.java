package com.telcocrm.billingservice.service;

import com.telcocrm.billingservice.client.ProductCatalogClient;
import com.telcocrm.billingservice.entity.BillCycle;
import com.telcocrm.billingservice.entity.Invoice;
import com.telcocrm.billingservice.entity.InvoiceLine;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import com.telcocrm.billingservice.event.InvoiceGeneratedEvent;
import com.telcocrm.billingservice.repository.BillCycleRepository;
import com.telcocrm.billingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillRunService {

    private final BillCycleRepository billCycleRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductCatalogClient productCatalogClient;
    private final OutboxService outboxService;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("20.00");

    @Transactional
    public int runBillCycle(LocalDate asOf) {
        List<BillCycle> dueCycles = billCycleRepository.findByActiveTrueAndNextRunDateLessThanEqual(asOf);
        log.info("Bill-run started: {} cycles due as of {}", dueCycles.size(), asOf);

        int generated = 0;
        for (BillCycle cycle : dueCycles) {
            try {
                generateInvoice(cycle, asOf);
                generated++;
            } catch (Exception e) {
                log.error("Failed to generate invoice for bill cycle {}: {}", cycle.getId(), e.getMessage(), e);
            }
        }

        log.info("Bill-run completed: {} invoices generated out of {} cycles", generated, dueCycles.size());
        return generated;
    }

    @Transactional
    public void createBillCycle(UUID customerId, UUID subscriptionId, String tariffCode, int dayOfMonth) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate nextRun = currentMonth.atDay(Math.min(dayOfMonth, currentMonth.lengthOfMonth()));

        BillCycle cycle = BillCycle.builder()
                .customerId(customerId)
                .subscriptionId(subscriptionId)
                .tariffCode(tariffCode)
                .dayOfMonth(dayOfMonth)
                .nextRunDate(nextRun)
                .active(true)
                .build();

        billCycleRepository.save(cycle);
        log.info("Bill cycle created for customer {}, subscription {}, next run: {}", customerId, subscriptionId, nextRun);
    }

    private void generateInvoice(BillCycle cycle, LocalDate asOf) {
        YearMonth billingMonth = YearMonth.from(asOf.minusMonths(1));
        LocalDate periodStart = billingMonth.atDay(1);
        LocalDate periodEnd = billingMonth.atEndOfMonth();

        if (invoiceRepository.existsBySubscriptionIdAndPeriodStartAndPeriodEnd(cycle.getSubscriptionId(), periodStart, periodEnd)) {
            log.info("Invoice already exists for subscription {} period {}-{}, skipping", cycle.getSubscriptionId(), periodStart, periodEnd);
            advanceCycle(cycle);
            return;
        }

        Map<String, Object> tariff = productCatalogClient.getTariff(cycle.getTariffCode());
        BigDecimal monthlyFee = parseBigDecimal(tariff.get("monthlyFee"));

        Invoice invoice = Invoice.builder()
                .customerId(cycle.getCustomerId())
                .subscriptionId(cycle.getSubscriptionId())
                .invoiceNumber(generateInvoiceNumber(billingMonth))
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .taxRate(DEFAULT_TAX_RATE)
                .status(InvoiceStatus.DRAFT)
                .dueDate(periodEnd.plusDays(14))
                .lines(new ArrayList<>())
                .build();

        InvoiceLine tariffLine = InvoiceLine.builder()
                .invoice(invoice)
                .description("Aylık Tarife Ücreti - " + cycle.getTariffCode())
                .quantity(1)
                .unitPrice(monthlyFee)
                .lineTotal(monthlyFee)
                .build();
        invoice.getLines().add(tariffLine);

        BigDecimal subTotal = monthlyFee;
        BigDecimal taxAmount = subTotal.multiply(DEFAULT_TAX_RATE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subTotal.add(taxAmount);

        invoice.setSubTotal(subTotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.ISSUED);

        Invoice saved = invoiceRepository.save(invoice);

        outboxService.saveEvent(
                "INVOICE",
                saved.getId().toString(),
                "invoice-generated-topic",
                InvoiceGeneratedEvent.of(
                        saved.getId(),
                        saved.getInvoiceNumber(),
                        saved.getCustomerId(),
                        saved.getSubscriptionId(),
                        saved.getGrandTotal(),
                        null, null, null
                )
        );

        log.info("Invoice {} generated for customer {}, total: {}", saved.getInvoiceNumber(), cycle.getCustomerId(), grandTotal);

        advanceCycle(cycle);
    }

    private void advanceCycle(BillCycle cycle) {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        LocalDate nextRun = nextMonth.atDay(Math.min(cycle.getDayOfMonth(), nextMonth.lengthOfMonth()));
        cycle.setNextRunDate(nextRun);
        billCycleRepository.save(cycle);
    }

    private String generateInvoiceNumber(YearMonth month) {
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%d%02d-%04d", month.getYear(), month.getMonthValue(), count);
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
