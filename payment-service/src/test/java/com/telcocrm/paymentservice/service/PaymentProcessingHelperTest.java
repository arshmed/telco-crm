package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingHelperTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private MockPspClient mockPspClient;

    @InjectMocks
    private PaymentProcessingHelper paymentProcessingHelper;

    @Test
    void attemptInitialCharge_shouldCompletePaymentOnSuccess() {
        Payment payment = buildPendingPayment();
        when(mockPspClient.charge(payment.getAmount(), payment.getMethod()))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-abc", null));

        PspChargeResult result = paymentProcessingHelper.attemptInitialCharge(payment);

        assertThat(result.success()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getExternalRef()).isEqualTo("MOCK-REF-abc");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getAttempts()).hasSize(1);
        assertThat(payment.getAttempts().get(0).getAttemptNo()).isEqualTo(1);
        assertThat(payment.getAttempts().get(0).getResponse()).isEqualTo("MOCK_PSP_APPROVED");

        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(paymentRepository).save(payment);
        verify(outboxService).saveEvent(eq("PAYMENT"), eq(payment.getId().toString()), eq("payment-completed-topic"), any());
    }

    @Test
    void attemptInitialCharge_shouldMarkFailedAndScheduleRetryOnFailure() {
        Payment payment = buildPendingPayment();
        when(mockPspClient.charge(payment.getAmount(), payment.getMethod()))
                .thenReturn(new PspChargeResult(false, null, "Card declined"));

        PspChargeResult result = paymentProcessingHelper.attemptInitialCharge(payment);

        assertThat(result.success()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Card declined");
        assertThat(payment.getRetryCount()).isEqualTo(1);
        assertThat(payment.getNextRetryAt()).isNotNull().isAfter(Instant.now());
        assertThat(payment.getAttempts()).hasSize(1);
        assertThat(payment.getAttempts().get(0).getResponse()).isEqualTo("Card declined");

        verify(paymentRepository).save(payment);
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void attemptInitialCharge_shouldLinkAttemptBackToPayment() {
        Payment payment = buildPendingPayment();
        when(mockPspClient.charge(any(), any())).thenReturn(new PspChargeResult(true, "MOCK-REF-xyz", null));

        paymentProcessingHelper.attemptInitialCharge(payment);

        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getPayment()).isSameAs(payment);
    }

    private Payment buildPendingPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("149.90"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();
    }
}
