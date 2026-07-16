package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.ProcessedEvent;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.repository.ProcessedEventRepository;
import com.telcocrm.paymentservice.service.impl.PaymentEventProcessingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProcessingServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private PaymentAuditService paymentAuditService;

    @InjectMocks
    private PaymentEventProcessingServiceImpl eventProcessingService;

    @Test
    void processSubscriptionActivationFailed_shouldRefundCompletedPayment() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        SubscriptionActivationFailedEvent event = new SubscriptionActivationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), orderId, "Activation failed");

        Payment payment = buildPayment(paymentId, orderId, PaymentStatus.COMPLETED);

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        eventProcessingService.processSubscriptionActivationFailed(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
        verify(outboxService).saveEvent(eq("PAYMENT"), eq(paymentId.toString()), eq("payment-refunded-topic"), any());
        verify(paymentAuditService).log(eq(payment), contains(orderId.toString()));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void processSubscriptionActivationFailed_shouldSkipDuplicateEvent() {
        SubscriptionActivationFailedEvent event = new SubscriptionActivationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "Failed");

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        eventProcessingService.processSubscriptionActivationFailed(event);

        verify(paymentRepository, never()).findByOrderId(any());
    }

    @Test
    void processSubscriptionActivationFailed_shouldSkipWhenNoPaymentFound() {
        UUID orderId = UUID.randomUUID();
        SubscriptionActivationFailedEvent event = new SubscriptionActivationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), orderId, "Activation failed");

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        eventProcessingService.processSubscriptionActivationFailed(event);

        verify(paymentRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    void processSubscriptionActivationFailed_shouldSkipWhenPaymentNotCompleted() {
        UUID orderId = UUID.randomUUID();
        SubscriptionActivationFailedEvent event = new SubscriptionActivationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), orderId, "Activation failed");
        Payment payment = buildPayment(UUID.randomUUID(), orderId, PaymentStatus.FAILED);

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        eventProcessingService.processSubscriptionActivationFailed(event);

        verify(paymentRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        verify(paymentAuditService, never()).log(any(), any());
    }

    private Payment buildPayment(UUID id, UUID orderId, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("149.99"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }
}
