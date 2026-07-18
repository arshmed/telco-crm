package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.client.OrderClient;
import com.telcocrm.paymentservice.client.dto.OrderResponse;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.publish.PaymentRefundedEvent;
import com.telcocrm.paymentservice.exception.DuplicateRequestException;
import com.telcocrm.paymentservice.exception.OrderNotPayableException;
import com.telcocrm.paymentservice.exception.PaymentAlreadyProcessedException;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.mapper.PaymentMapper;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentAuditService;
import com.telcocrm.paymentservice.service.PaymentProcessingHelper;
import com.telcocrm.paymentservice.security.CustomerAccessGuard;
import com.telcocrm.paymentservice.service.PaymentService;
import com.telcocrm.paymentservice.util.CardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PENDING_PAYMENT_STATUS = "PENDING_PAYMENT";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxService outboxService;
    private final OrderClient orderClient;
    private final PaymentProcessingHelper paymentProcessingHelper;
    private final PaymentAuditService paymentAuditService;
    private final CustomerAccessGuard customerAccessGuard;

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
        customerAccessGuard.assertOwnResource(payment.getCustomerId(), () -> new PaymentNotFoundException(paymentId));

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

        payment.setStatus(PaymentStatus.REFUNDED);

        paymentRepository.save(payment);

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
        if (!CardValidator.isValidLuhn(request.cardNumber())) {
            throw new IllegalArgumentException("Card number failed Luhn validation");
        }
        if (!CardValidator.isExpiryValid(request.expiryDate())) {
            throw new IllegalArgumentException("Card expiry date is invalid or in the past");
        }

        Optional<Payment> existingByRequestId = paymentRepository.findByPaymentRequestId(request.paymentRequestId());
        if (existingByRequestId.isPresent()) {
            log.info("Replaying existing payment for paymentRequestId: {}", request.paymentRequestId());
            return paymentMapper.toResponse(existingByRequestId.get());
        }

        // Tutar/para birimi/customerId frontend'den değil, her zaman order-service'ten (source of truth) alınır
        OrderResponse order = orderClient.getOrderById(request.orderId());
        if (!PENDING_PAYMENT_STATUS.equals(order.status())) {
            throw new OrderNotPayableException(order.id(), order.status());
        }

        Payment payment = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyProcessedException(request.orderId().toString());
        }

        if (payment == null) {
            payment = Payment.builder()
                    .paymentRequestId(request.paymentRequestId())
                    .orderId(order.id())
                    .customerId(order.customerId())
                    .amount(order.totalAmount())
                    .currency(order.currency())
                    .method(request.method())
                    .status(PaymentStatus.PENDING)
                    .build();
            try {
                paymentRepository.saveAndFlush(payment);
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateRequestException(request.paymentRequestId());
            }
        } else {
            // Önceki deneme FAILED oldu — aynı sipariş için yeni paymentRequestId ile tekrar deneniyor
            payment.setPaymentRequestId(request.paymentRequestId());
            payment.setMethod(request.method());
            payment.setFailureReason(null);
        }

        PspChargeResult chargeResult = paymentProcessingHelper.attemptInitialCharge(payment, request.cardNumber());

        paymentAuditService.log(payment, chargeResult.success()
                ? "Payment created and completed via API"
                : "Payment created but failed via API: " + chargeResult.failureReason());

        return paymentMapper.toResponse(payment);
    }
}
