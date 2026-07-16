package com.telcocrm.paymentservice.scheduler;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRetrySchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private MockPspClient mockPspClient;
    @Mock
    private PaymentAuditService paymentAuditService;

    @InjectMocks
    private PaymentRetryScheduler paymentRetryScheduler;

    @Test
    void retryFailedPayments_shouldDoNothingWhenNoPaymentsAreDue() {
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of());

        paymentRetryScheduler.retryFailedPayments();

        verify(mockPspClient, never()).charge(any(), any(), any());
    }

    @Test
    void retryFailedPayments_shouldProcessEveryDuePayment() {
        Payment first = buildDuePayment(1);
        Payment second = buildDuePayment(1);
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of(first, second));
        when(mockPspClient.charge(any(), any(), any())).thenReturn(new PspChargeResult(true, "MOCK-REF-1", null));

        paymentRetryScheduler.retryFailedPayments();

        verify(mockPspClient, times(2)).charge(any(), any(), any());
    }

    @Test
    void retry_shouldCompletePaymentWhenChargeSucceeds() {
        Payment payment = buildDuePayment(1);
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of(payment));
        when(mockPspClient.charge(payment.getAmount(), payment.getMethod(), null))
                .thenReturn(new PspChargeResult(true, "MOCK-REF-success", null));

        paymentRetryScheduler.retryFailedPayments();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getExternalRef()).isEqualTo("MOCK-REF-success");
        assertThat(payment.getNextRetryAt()).isNull();
        assertThat(payment.getAttempts()).hasSize(1);
        assertThat(payment.getAttempts().get(0).getAttemptNo()).isEqualTo(2);

        verify(outboxService).saveEvent(eq("PAYMENT"), eq(payment.getId().toString()), eq("payment-completed-topic"), any());
        verify(paymentAuditService).log(eq(payment), org.mockito.ArgumentMatchers.contains("succeeded"));
    }

    @Test
    void retry_shouldRescheduleWithSeventyTwoHoursOnSecondFailure() {
        Payment payment = buildDuePayment(1);
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of(payment));
        when(mockPspClient.charge(any(), any(), any())).thenReturn(new PspChargeResult(false, null, "Card declined"));

        paymentRetryScheduler.retryFailedPayments();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getRetryCount()).isEqualTo(2);
        assertThat(payment.getNextRetryAt())
                .isCloseTo(Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS), org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void retry_shouldRescheduleWithOneHundredSixtyEightHoursOnThirdFailure() {
        Payment payment = buildDuePayment(2);
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of(payment));
        when(mockPspClient.charge(any(), any(), any())).thenReturn(new PspChargeResult(false, null, "Card expired"));

        paymentRetryScheduler.retryFailedPayments();

        assertThat(payment.getRetryCount()).isEqualTo(3);
        assertThat(payment.getNextRetryAt())
                .isCloseTo(Instant.now().plus(168, java.time.temporal.ChronoUnit.HOURS), org.assertj.core.api.Assertions.within(5, java.time.temporal.ChronoUnit.SECONDS));
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void retry_shouldPermanentlyFailAfterFourthAttempt() {
        Payment payment = buildDuePayment(3);
        when(paymentRepository.findByStatusAndNextRetryAtBefore(eq(PaymentStatus.FAILED), any()))
                .thenReturn(List.of(payment));
        when(mockPspClient.charge(any(), any(), any())).thenReturn(new PspChargeResult(false, null, "Insufficient funds"));

        paymentRetryScheduler.retryFailedPayments();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getRetryCount()).isEqualTo(4);
        assertThat(payment.getNextRetryAt()).isNull();
        verify(outboxService).saveEvent(eq("PAYMENT"), eq(payment.getId().toString()), eq("payment-failed-topic"), any());
        verify(paymentAuditService).log(eq(payment), org.mockito.ArgumentMatchers.contains("permanently failed"));
    }

    private Payment buildDuePayment(int retryCount) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("75.00"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.FAILED)
                .retryCount(retryCount)
                .nextRetryAt(Instant.now().minusSeconds(60))
                .build();
    }
}
