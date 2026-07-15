package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.publish.PaymentRefundedEvent;
import com.telcocrm.paymentservice.exception.DuplicateRequestException;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.mapper.PaymentMapper;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentAuditService;
import com.telcocrm.paymentservice.service.PaymentProcessingHelper;
import com.telcocrm.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxService outboxService;
    private final PaymentProcessingHelper paymentProcessingHelper;
    private final PaymentAuditService paymentAuditService;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentRefundException(paymentId, "Only completed payments can be refunded");
        }

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.REFUNDED);

        paymentRepository.save(payment);

        auditListener.logUpdate("Payment", payment.getId().toString(),
                Map.of("status", oldStatus), Map.of("status", PaymentStatus.REFUNDED));

        outboxService.saveEvent(
                "PAYMENT",
                payment.getId().toString(),
                "payment-refunded-topic",
                PaymentRefundedEvent.of(payment.getOrderId(), payment.getId(), payment.getAmount())
        );

        paymentAuditService.log(payment, "Payment refunded via API, reason: " + request.reason());

        log.info("Payment refunded for paymentId: {}, reason: {}", payment.getId(), request.reason());

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByPaymentRequestId(request.paymentRequestId());
        if (existing.isPresent()) {
            log.info("Replaying existing payment for paymentRequestId: {}", request.paymentRequestId());
            return paymentMapper.toResponse(existing.get());
        }

        Payment payment = Payment.builder()
                .paymentRequestId(request.paymentRequestId())
                .orderId(request.orderId())
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .build();

        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRequestException(request.paymentRequestId());
        }

        PspChargeResult chargeResult = paymentProcessingHelper.attemptInitialCharge(payment);

        paymentAuditService.log(payment, chargeResult.success()
                ? "Payment created and completed via API"
                : "Payment created but failed via API: " + chargeResult.failureReason());

        return paymentMapper.toResponse(payment);
    }
}
