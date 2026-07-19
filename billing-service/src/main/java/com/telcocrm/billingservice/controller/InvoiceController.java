package com.telcocrm.billingservice.controller;

import com.telcocrm.billingservice.dto.BillCycleResponse;
import com.telcocrm.billingservice.dto.InvoiceResponse;
import com.telcocrm.billingservice.dto.InvoiceStatsResponse;
import com.telcocrm.billingservice.entity.BillCycle;
import com.telcocrm.billingservice.pdf.InvoicePdfGenerator;
import com.telcocrm.billingservice.repository.BillCycleRepository;
import com.telcocrm.billingservice.service.InvoiceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final BillCycleRepository billCycleRepository;
    private final InvoicePdfGenerator invoicePdfGenerator;

    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceResponse>> listInvoices(
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoices(customerId, pageable));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @GetMapping("/invoices/{id}/pdf")
    public void downloadInvoicePdf(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        InvoiceResponse invoice = invoiceService.getInvoice(id);
        byte[] pdfBytes = invoicePdfGenerator.generate(invoice);

        String filename = invoice.getInvoiceNumber() + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/invoices/stats")
    public ResponseEntity<InvoiceStatsResponse> getStats() {
        return ResponseEntity.ok(invoiceService.getStats());
    }

    @GetMapping("/billing/cycles")
    public ResponseEntity<List<BillCycleResponse>> listBillCycles(@RequestParam UUID customerId) {
        var cycles = billCycleRepository.findByCustomerId(customerId)
                .stream()
                .map(c -> BillCycleResponse.builder()
                        .id(c.getId())
                        .customerId(c.getCustomerId())
                        .subscriptionId(c.getSubscriptionId())
                        .tariffCode(c.getTariffCode())
                        .dayOfMonth(c.getDayOfMonth())
                        .nextRunDate(c.getNextRunDate())
                        .active(c.getActive())
                        .build())
                .toList();
        return ResponseEntity.ok(cycles);
    }
}
