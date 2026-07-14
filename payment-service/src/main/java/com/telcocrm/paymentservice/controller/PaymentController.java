package com.telcocrm.paymentservice.controller;

import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {
        PaymentResponse response = paymentService.refundPayment(id, request);
        return ResponseEntity.ok(response);
    }
}
