package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.publish.PaymentCompletedEvent;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class PaymentProcessingHelper {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OutboxService outboxService;
    private final MockPspClient mockPspClient;

    public PspChargeResult attemptInitialCharge(Payment payment, String cardNumber) {
        PspChargeResult chargeResult = mockPspClient.charge(payment.getAmount(), payment.getMethod(), cardNumber);

        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(payment.getAttempts().size() + 1)
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
                PaymentCompletedEvent.of(payment.getOrderId(), payment.getId(), null)
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(chargeResult.failureReason());
            payment.setRetryCount(1);
            payment.setNextRetryAt(Instant.now().plus(24, ChronoUnit.HOURS));

            paymentRepository.save(payment);
        }

        return chargeResult;
    }
}
