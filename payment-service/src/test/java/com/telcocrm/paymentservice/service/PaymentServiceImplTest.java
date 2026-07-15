package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.client.OrderClient;
import com.telcocrm.paymentservice.client.dto.OrderResponse;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.exception.OrderNotPayableException;
import com.telcocrm.paymentservice.exception.PaymentAlreadyProcessedException;
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

    private static final String VALID_CARD_NUMBER = "4242424242424242";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private OutboxService outboxService;
    @Mock
    private OrderClient orderClient;
    @Mock
    private PaymentProcessingHelper paymentProcessingHelper;
    @Mock
    private PaymentAuditService paymentAuditService;

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
        verify(paymentAuditService).log(eq(payment), any());
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

    @Test
    void createPayment_shouldCompleteOnSuccessfulCharge() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", orderId, PaymentMethod.CREDIT_CARD, "Ali Veli", VALID_CARD_NUMBER, "12/99", "123");

        OrderResponse order = new OrderResponse(orderId, customerId, "PENDING_PAYMENT", new BigDecimal("149.99"), "TRY");

        when(paymentRepository.findByPaymentRequestId("req-1")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(orderId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentProcessingHelper.attemptInitialCharge(any(Payment.class), eq(VALID_CARD_NUMBER)))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-123", null));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(buildPaymentResponse(UUID.randomUUID(), PaymentStatus.COMPLETED));

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isNotNull();
        verify(paymentAuditService).log(any(Payment.class), any());
    }

    @Test
    void createPayment_shouldReplayExistingPaymentRequestId() {
        UUID orderId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", orderId, PaymentMethod.CREDIT_CARD, "Ali Veli", VALID_CARD_NUMBER, "12/99", "123");

        Payment existing = buildPayment(UUID.randomUUID(), PaymentStatus.COMPLETED);
        when(paymentRepository.findByPaymentRequestId("req-1")).thenReturn(Optional.of(existing));
        when(paymentMapper.toResponse(existing)).thenReturn(buildPaymentResponse(existing.getId(), PaymentStatus.COMPLETED));

        paymentService.createPayment(request);

        verifyNoInteractions(orderClient);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createPayment_shouldRejectInvalidLuhnCardNumber() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", UUID.randomUUID(), PaymentMethod.CREDIT_CARD, "Ali Veli", "1234567812345678", "12/99", "123");

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderClient);
    }

    @Test
    void createPayment_shouldThrowWhenOrderNotPayable() {
        UUID orderId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", orderId, PaymentMethod.CREDIT_CARD, "Ali Veli", VALID_CARD_NUMBER, "12/99", "123");

        OrderResponse order = new OrderResponse(orderId, UUID.randomUUID(), "FULFILLED", new BigDecimal("149.99"), "TRY");
        when(paymentRepository.findByPaymentRequestId("req-1")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(orderId)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(OrderNotPayableException.class);

        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createPayment_shouldThrowWhenAlreadyCompleted() {
        UUID orderId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", orderId, PaymentMethod.CREDIT_CARD, "Ali Veli", VALID_CARD_NUMBER, "12/99", "123");

        OrderResponse order = new OrderResponse(orderId, UUID.randomUUID(), "PENDING_PAYMENT", new BigDecimal("149.99"), "TRY");
        when(paymentRepository.findByPaymentRequestId("req-1")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(orderId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(buildPayment(UUID.randomUUID(), PaymentStatus.COMPLETED)));

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
    }

    @Test
    void createPayment_shouldRetryOnPreviousFailedPayment() {
        UUID orderId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-2", orderId, PaymentMethod.CREDIT_CARD, "Ali Veli", VALID_CARD_NUMBER, "12/99", "123");

        OrderResponse order = new OrderResponse(orderId, UUID.randomUUID(), "PENDING_PAYMENT", new BigDecimal("149.99"), "TRY");
        Payment failedPayment = buildPayment(UUID.randomUUID(), PaymentStatus.FAILED);
        failedPayment.setOrderId(orderId);

        when(paymentRepository.findByPaymentRequestId("req-2")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(orderId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(failedPayment));
        when(paymentProcessingHelper.attemptInitialCharge(eq(failedPayment), eq(VALID_CARD_NUMBER)))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-456", null));
        when(paymentMapper.toResponse(failedPayment)).thenReturn(buildPaymentResponse(failedPayment.getId(), PaymentStatus.COMPLETED));

        paymentService.createPayment(request);

        verify(paymentRepository, never()).saveAndFlush(any());
        assertThat(failedPayment.getPaymentRequestId()).isEqualTo("req-2");
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
