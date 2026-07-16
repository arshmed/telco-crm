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
import com.telcocrm.paymentservice.exception.DuplicateRequestException;
import com.telcocrm.paymentservice.exception.OrderNotPayableException;
import com.telcocrm.paymentservice.exception.PaymentAlreadyProcessedException;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.mapper.PaymentMapper;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String VALID_CARD_NUMBER = "4242424242424242";
    private static final String VALID_EXPIRY = "12/99";
    private static final String VALID_CVV = "123";
    private static final String PENDING_PAYMENT_STATUS = "PENDING_PAYMENT";

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
    void getAllPayments_shouldReturnMappedPage() {
        Payment payment = buildPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.COMPLETED);
        PaymentResponse response = buildPaymentResponse(payment.getId(), PaymentStatus.COMPLETED);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);

        when(paymentRepository.findAll(pageable)).thenReturn(page);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        Page<PaymentResponse> result = paymentService.getAllPayments(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllPayments_shouldReturnEmptyPageWhenNoPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(paymentRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<PaymentResponse> result = paymentService.getAllPayments(pageable);

        assertThat(result.getContent()).isEmpty();
        verify(paymentMapper, never()).toResponse(any(Payment.class));
    }

    @Test
    void getPaymentById_shouldReturnPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, UUID.randomUUID(), PaymentStatus.COMPLETED);
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
        Payment payment = buildPayment(paymentId, UUID.randomUUID(), PaymentStatus.COMPLETED);
        PaymentResponse response = buildPaymentResponse(paymentId, PaymentStatus.REFUNDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        RefundRequest request = new RefundRequest("Customer requested");
        PaymentResponse result = paymentService.refundPayment(paymentId, request);

        assertThat(result).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
        verify(outboxService).saveEvent(eq("PAYMENT"), eq(paymentId.toString()),
                eq("payment-refunded-topic"), any());
        verify(paymentAuditService).log(eq(payment), contains("Customer requested"));
    }

    @Test
    void refundPayment_shouldThrowWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentNotFoundException.class);

        verify(paymentAuditService, never()).log(any(), any());
    }

    @Test
    void refundPayment_shouldThrowWhenPaymentNotCompleted() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, UUID.randomUUID(), PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentRefundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldThrowWhenAlreadyRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildPayment(paymentId, UUID.randomUUID(), PaymentStatus.REFUNDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        RefundRequest request = new RefundRequest("Reason");
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(PaymentRefundException.class);
    }

    @Test
    void createPayment_shouldThrowWhenCardNumberFailsLuhnCheck() {
        CreatePaymentRequest request = buildCreateRequest("req-1", "1234567890123456", VALID_EXPIRY);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderClient);
        verify(paymentRepository, never()).findByPaymentRequestId(any());
    }

    @Test
    void createPayment_shouldThrowWhenExpiryDateIsInThePast() {
        CreatePaymentRequest request = buildCreateRequest("req-1", VALID_CARD_NUMBER, "01/20");

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(orderClient);
    }

    @Test
    void createPayment_shouldReplayExistingPaymentWithoutCharging() {
        CreatePaymentRequest request = buildCreateRequest("req-1");
        Payment existing = buildPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.COMPLETED);
        PaymentResponse response = buildPaymentResponse(existing.getId(), PaymentStatus.COMPLETED);

        when(paymentRepository.findByPaymentRequestId("req-1")).thenReturn(Optional.of(existing));
        when(paymentMapper.toResponse(existing)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isEqualTo(response);
        verifyNoInteractions(orderClient);
        verify(paymentRepository, never()).saveAndFlush(any());
        verify(paymentProcessingHelper, never()).attemptInitialCharge(any(), any());
        verify(paymentAuditService, never()).log(any(), any());
    }

    @Test
    void createPayment_shouldThrowOrderNotPayableWhenOrderStatusIsNotPendingPayment() {
        CreatePaymentRequest request = buildCreateRequest("req-2");
        OrderResponse order = buildOrder(request.orderId(), "COMPLETED");

        when(paymentRepository.findByPaymentRequestId("req-2")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(OrderNotPayableException.class);

        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentProcessingHelper, never()).attemptInitialCharge(any(), any());
    }

    @Test
    void createPayment_shouldThrowPaymentAlreadyProcessedWhenExistingPaymentIsCompleted() {
        CreatePaymentRequest request = buildCreateRequest("req-3");
        OrderResponse order = buildOrder(request.orderId(), PENDING_PAYMENT_STATUS);
        Payment existingCompleted = buildPayment(UUID.randomUUID(), request.orderId(), PaymentStatus.COMPLETED);

        when(paymentRepository.findByPaymentRequestId("req-3")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);
        when(paymentRepository.findByOrderId(request.orderId())).thenReturn(Optional.of(existingCompleted));

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(PaymentAlreadyProcessedException.class);

        verify(paymentProcessingHelper, never()).attemptInitialCharge(any(), any());
    }

    @Test
    void createPayment_shouldCreateAndChargeNewPaymentSuccessfully() {
        CreatePaymentRequest request = buildCreateRequest("req-4");
        OrderResponse order = buildOrder(request.orderId(), PENDING_PAYMENT_STATUS);
        PaymentResponse response = buildPaymentResponse(UUID.randomUUID(), PaymentStatus.COMPLETED);

        when(paymentRepository.findByPaymentRequestId("req-4")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);
        when(paymentRepository.findByOrderId(request.orderId())).thenReturn(Optional.empty());
        when(paymentProcessingHelper.attemptInitialCharge(any(Payment.class), eq(VALID_CARD_NUMBER)))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-1", null));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getPaymentRequestId()).isEqualTo("req-4");
        assertThat(saved.getOrderId()).isEqualTo(order.id());
        assertThat(saved.getCustomerId()).isEqualTo(order.customerId());
        assertThat(saved.getAmount()).isEqualByComparingTo(order.totalAmount());
        assertThat(saved.getCurrency()).isEqualTo(order.currency());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentProcessingHelper).attemptInitialCharge(saved, VALID_CARD_NUMBER);
        verify(paymentAuditService).log(saved, "Payment created and completed via API");
    }

    @Test
    void createPayment_shouldAuditFailureWhenChargeFails() {
        CreatePaymentRequest request = buildCreateRequest("req-5");
        OrderResponse order = buildOrder(request.orderId(), PENDING_PAYMENT_STATUS);
        PaymentResponse response = buildPaymentResponse(UUID.randomUUID(), PaymentStatus.FAILED);

        when(paymentRepository.findByPaymentRequestId("req-5")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);
        when(paymentRepository.findByOrderId(request.orderId())).thenReturn(Optional.empty());
        when(paymentProcessingHelper.attemptInitialCharge(any(Payment.class), eq(VALID_CARD_NUMBER)))
                .thenReturn(new PspChargeResult(false, null, "Card declined"));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isEqualTo(response);
        verify(paymentAuditService).log(any(Payment.class), contains("Card declined"));
    }

    @Test
    void createPayment_shouldThrowDuplicateRequestExceptionOnConcurrentInsert() {
        CreatePaymentRequest request = buildCreateRequest("req-6");
        OrderResponse order = buildOrder(request.orderId(), PENDING_PAYMENT_STATUS);

        when(paymentRepository.findByPaymentRequestId("req-6")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);
        when(paymentRepository.findByOrderId(request.orderId())).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(DuplicateRequestException.class);

        verify(paymentProcessingHelper, never()).attemptInitialCharge(any(), any());
        verify(paymentAuditService, never()).log(any(), any());
    }

    @Test
    void createPayment_shouldRetryExistingFailedPaymentWithoutInsertingNewRow() {
        CreatePaymentRequest request = buildCreateRequest("req-7");
        OrderResponse order = buildOrder(request.orderId(), PENDING_PAYMENT_STATUS);
        Payment existingFailed = buildPayment(UUID.randomUUID(), request.orderId(), PaymentStatus.FAILED);
        existingFailed.setFailureReason("Card declined");
        PaymentResponse response = buildPaymentResponse(existingFailed.getId(), PaymentStatus.COMPLETED);

        when(paymentRepository.findByPaymentRequestId("req-7")).thenReturn(Optional.empty());
        when(orderClient.getOrderById(request.orderId())).thenReturn(order);
        when(paymentRepository.findByOrderId(request.orderId())).thenReturn(Optional.of(existingFailed));
        when(paymentProcessingHelper.attemptInitialCharge(existingFailed, VALID_CARD_NUMBER))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-retry", null));
        when(paymentMapper.toResponse(existingFailed)).thenReturn(response);

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isEqualTo(response);
        assertThat(existingFailed.getPaymentRequestId()).isEqualTo("req-7");
        assertThat(existingFailed.getFailureReason()).isNull();
        verify(paymentRepository, never()).saveAndFlush(any());
        verify(paymentProcessingHelper).attemptInitialCharge(existingFailed, VALID_CARD_NUMBER);
        verify(paymentAuditService).log(existingFailed, "Payment created and completed via API");
    }

    private CreatePaymentRequest buildCreateRequest(String paymentRequestId) {
        return buildCreateRequest(paymentRequestId, VALID_CARD_NUMBER, VALID_EXPIRY);
    }

    private CreatePaymentRequest buildCreateRequest(String paymentRequestId, String cardNumber, String expiryDate) {
        return new CreatePaymentRequest(
                paymentRequestId,
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                "Ali Veli",
                cardNumber,
                expiryDate,
                VALID_CVV
        );
    }

    private OrderResponse buildOrder(UUID orderId, String status) {
        return new OrderResponse(orderId, UUID.randomUUID(), status, new BigDecimal("99.99"), "TRY");
    }

    private Payment buildPayment(UUID id, UUID orderId, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
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
                List.of(),
                Instant.now(), Instant.now()
        );
    }
}
