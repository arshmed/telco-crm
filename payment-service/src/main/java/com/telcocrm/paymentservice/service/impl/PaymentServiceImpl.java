package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.OrderClient;
import com.telcocrm.paymentservice.client.dto.OrderResponse;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.config.PaymentAuditListener;
import com.telcocrm.paymentservice.dto.request.InitiatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.publish.PaymentCompletedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentFailedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentRefundedEvent;
import com.telcocrm.paymentservice.exception.OrderNotPayableException;
import com.telcocrm.paymentservice.exception.PaymentAlreadyProcessedException;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.mapper.PaymentMapper;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentService;
import com.telcocrm.paymentservice.util.CardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PENDING_PAYMENT_STATUS = "PENDING_PAYMENT";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxService outboxService;
    private final PaymentAuditListener auditListener;
    private final OrderClient orderClient;
    private final MockPspClient mockPspClient;

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(paymentMapper::toResponse);
    }

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

        log.info("Payment refunded for paymentId: {}, reason: {}", payment.getId(), request.reason());

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        if (!CardValidator.isValidLuhn(request.cardNumber())) {
            throw new IllegalArgumentException("Card number failed Luhn validation");
        }
        if (!CardValidator.isExpiryValid(request.expiryDate())) {
            throw new IllegalArgumentException("Card expiry date is invalid or in the past");
        }

        // Tutar/para birimi frontend'den değil, her zaman order-service'ten (source of truth) alınır
        OrderResponse order = orderClient.getOrderById(request.orderId());
        if (!PENDING_PAYMENT_STATUS.equals(order.status())) {
            throw new OrderNotPayableException(order.id(), order.status());
        }

        Payment payment = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyProcessedException(request.orderId().toString());
        }

        boolean isNewPayment = payment == null;
        if (isNewPayment) {
            payment = Payment.builder()
                    .orderId(order.id())
                    .customerId(order.customerId())
                    .amount(order.totalAmount())
                    .currency(order.currency())
                    .method(request.method())
                    .status(PaymentStatus.PENDING)
                    .build();
            payment = paymentRepository.save(payment);
        }

        PspChargeResult chargeResult = mockPspClient.charge(payment.getAmount(), payment.getMethod(), request.cardNumber());

        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(payment.getAttempts().size() + 1)
                .response(chargeResult.success() ? "MOCK_PSP_APPROVED" : chargeResult.failureReason())
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);
        paymentAttemptRepository.save(attempt);

        if (chargeResult.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(Instant.now());
            payment.setExternalRef(chargeResult.externalRef());
            payment.setFailureReason(null);
            paymentRepository.save(payment);

            auditListener.logCreate("Payment", payment.getId().toString(),
                    Map.of("status", payment.getStatus(), "orderId", payment.getOrderId()));

            outboxService.saveEvent(
                    "PAYMENT",
                    payment.getId().toString(),
                    "payment-completed-topic",
                    PaymentCompletedEvent.of(payment.getOrderId(), payment.getId())
            );

            log.info("Payment completed for orderId: {}", payment.getOrderId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(chargeResult.failureReason());
            paymentRepository.save(payment);

            auditListener.logCreate("Payment", payment.getId().toString(),
                    Map.of("status", payment.getStatus(), "failureReason", chargeResult.failureReason()));

            outboxService.saveEvent(
                    "PAYMENT",
                    payment.getId().toString(),
                    "payment-failed-topic",
                    PaymentFailedEvent.of(payment.getOrderId(), chargeResult.failureReason())
            );

            log.info("Payment failed for orderId: {}, reason: {}", payment.getOrderId(), chargeResult.failureReason());
        }

        return paymentMapper.toResponse(payment);
    }
}
