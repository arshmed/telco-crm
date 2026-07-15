package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.ProcessedEvent;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentRefundedEvent;
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
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public void processSubscriptionActivationFailed(SubscriptionActivationFailedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("SubscriptionActivationFailedEvent already processed: {}", event.eventId());
            return;
        }

        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            log.warn("No payment found for orderId: {}, skipping SubscriptionActivationFailedEvent {}",
                    event.orderId(), event.eventId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("Payment {} for orderId: {} is not COMPLETED (status={}), nothing to refund",
                    payment.getId(), event.orderId(), payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        paymentRepository.save(payment);

        outboxService.saveEvent(
            "PAYMENT",
            payment.getId().toString(),
            "payment-refunded-topic",
            PaymentRefundedEvent.of(payment.getOrderId(), payment.getId(), payment.getAmount())
        );

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("SubscriptionActivationFailed processed, payment refunded for orderId: {}", event.orderId());
    }
}
