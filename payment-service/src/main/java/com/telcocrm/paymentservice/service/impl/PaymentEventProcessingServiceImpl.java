package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.ProcessedEvent;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentCompletedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentFailedEvent;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.repository.ProcessedEventRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentEventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessingServiceImpl implements PaymentEventProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    private final MockPspClient mockPspClient;

    @Override
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("OrderCreatedEvent already processed: {}", event.eventId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .currency(event.currency())
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        PspChargeResult chargeResult = mockPspClient.charge(payment.getAmount(), payment.getMethod());

        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(1)
                .response(chargeResult.success() ? "MOCK_PSP_APPROVED" : chargeResult.failureReason())
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);
        paymentAttemptRepository.save(attempt);

        if (chargeResult.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(Instant.now());
            payment.setExternalRef(chargeResult.externalRef());

            paymentRepository.save(payment);

            outboxService.saveEvent(
                "PAYMENT",
                payment.getId().toString(),
                "payment-completed-topic",
                PaymentCompletedEvent.of(payment.getOrderId(), payment.getId())
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(chargeResult.failureReason());

            paymentRepository.save(payment);

            outboxService.saveEvent(
                "PAYMENT",
                payment.getId().toString(),
                "payment-failed-topic",
                PaymentFailedEvent.of(payment.getOrderId(), chargeResult.failureReason())
            );
        }

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("OrderCreated processed, payment {} for orderId: {}",
                chargeResult.success() ? "completed" : "failed", event.orderId());
    }
}
