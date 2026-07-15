package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.config.PaymentAuditListener;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.mapper.PaymentMapper;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private OutboxService outboxService;
    @Mock
    private PaymentAuditListener auditListener;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void getPaymentById_shouldReturnPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, PaymentStatus.COMPLETED);
        PaymentResponse response = buildPaymentResponse(paymentId, PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.getPaymentById(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(paymentId);
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_shouldThrowWhenNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void refundPayment_shouldRefundCompletedPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, PaymentStatus.COMPLETED);
        PaymentResponse response = buildPaymentResponse(paymentId, PaymentStatus.REFUNDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        RefundRequest request = new RefundRequest("Customer requested");
        PaymentResponse result = paymentService.refundPayment(paymentId, request);

        assertThat(result).isNotNull();
        verify(paymentRepository).save(payment);
        verify(outboxService).saveEvent(eq("PAYMENT"), eq(paymentId.toString()),
                eq("payment-refunded-topic"), any());
        verify(auditListener).logUpdate(eq("Payment"), eq(paymentId.toString()), any(), any());
    }

    @Test
    void refundPayment_shouldThrowWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void refundPayment_shouldThrowWhenPaymentNotCompleted() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentRefundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldThrowWhenAlreadyRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, PaymentStatus.REFUNDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentRefundException.class);
    }

    private Payment buildPayment(UUID id, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }

    private PaymentResponse buildPaymentResponse(UUID id, PaymentStatus status) {
        return new PaymentResponse(
                id, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("99.99"), "TRY",
                PaymentMethod.CREDIT_CARD, status,
                null, null, null,
                java.util.List.of(),
                Instant.now(), Instant.now()
        );
    }
}
