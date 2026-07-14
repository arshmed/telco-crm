package com.telcocrm.paymentservice.service.impl;

import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.ProcessedEvent;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentCompletedEvent;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessingServiceImpl implements PaymentEventProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;

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

        // TODO: mock PSP entegrasyonu buraya eklenecek, şimdilik her ödeme başarılı sayılıyor
        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(1)
                .response("MOCK_PSP_APPROVED")
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);
        paymentAttemptRepository.save(attempt);

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Instant.now());
        payment.setExternalRef("MOCK-REF-" + UUID.randomUUID());

        paymentRepository.save(payment);

        outboxService.saveEvent(
            "PAYMENT",
            payment.getId().toString(),
            "payment-completed-topic",
            PaymentCompletedEvent.of(payment.getOrderId(), payment.getId())
        );

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("OrderCreated processed, payment completed for orderId: {}", event.orderId());
    }
}
