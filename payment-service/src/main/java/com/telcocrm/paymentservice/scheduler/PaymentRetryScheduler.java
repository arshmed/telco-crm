package com.telcocrm.paymentservice.scheduler;

import com.telcocrm.paymentservice.client.MockPspClient;
import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.event.publish.PaymentCompletedEvent;
import com.telcocrm.paymentservice.event.publish.PaymentFailedEvent;
import com.telcocrm.paymentservice.repository.PaymentAttemptRepository;
import com.telcocrm.paymentservice.repository.PaymentRepository;
import com.telcocrm.paymentservice.service.OutboxService;
import com.telcocrm.paymentservice.service.PaymentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private static final int MAX_RETRY_COUNT = 3;

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OutboxService outboxService;
    private final MockPspClient mockPspClient;
    private final PaymentAuditService paymentAuditService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedPayments() {
        List<Payment> duePayments = paymentRepository.findByStatusAndNextRetryAtBefore(PaymentStatus.FAILED, Instant.now());

        for (Payment payment : duePayments) {
            retry(payment);
        }
    }

    private void retry(Payment payment) {
        int attemptNo = payment.getRetryCount() + 1;

        // Kart bilgisi saklanmıyor (PCI kapsamını daraltmak için) — otomatik retry'de kart no olmadan
        // yöntem bazlı mock PSP profiliyle deneniyor.
        PspChargeResult chargeResult = mockPspClient.charge(payment.getAmount(), payment.getMethod(), null);

        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNo(attemptNo)
                .response(chargeResult.success() ? "MOCK_PSP_APPROVED" : chargeResult.failureReason())
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);
        paymentAttemptRepository.save(attempt);

        if (chargeResult.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(Instant.now());
            payment.setExternalRef(chargeResult.externalRef());
            payment.setNextRetryAt(null);

            paymentRepository.save(payment);

            outboxService.saveEvent(
                "PAYMENT",
                payment.getId().toString(),
                "payment-completed-topic",
                PaymentCompletedEvent.of(payment.getOrderId(), payment.getId(), null)
            );

            paymentAuditService.log(payment, "Payment retry succeeded, attemptNo: " + attemptNo);

            log.info("Payment retry succeeded for paymentId: {}, attemptNo: {}", payment.getId(), attemptNo);
            return;
        }

        payment.setFailureReason(chargeResult.failureReason());

        if (payment.getRetryCount() < MAX_RETRY_COUNT) {
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setNextRetryAt(Instant.now().plus(nextDelayHours(payment.getRetryCount()), ChronoUnit.HOURS));

            paymentRepository.save(payment);

            paymentAuditService.log(payment, "Payment retry failed, attemptNo: " + attemptNo
                    + ", reason: " + chargeResult.failureReason() + ", next retry at: " + payment.getNextRetryAt());

            log.warn("Payment retry failed for paymentId: {}, attemptNo: {}, will retry at: {}",
                    payment.getId(), attemptNo, payment.getNextRetryAt());
        } else {
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setNextRetryAt(null);

            paymentRepository.save(payment);

            outboxService.saveEvent(
                "PAYMENT",
                payment.getId().toString(),
                "payment-failed-topic",
                PaymentFailedEvent.of(payment.getOrderId(), chargeResult.failureReason())
            );

            paymentAuditService.log(payment, "Payment retry permanently failed, attemptNo: " + attemptNo
                    + ", reason: " + chargeResult.failureReason());

            log.warn("Payment retry permanently failed for paymentId: {}, attemptNo: {}", payment.getId(), attemptNo);
        }
    }

    private long nextDelayHours(int newRetryCount) {
        return switch (newRetryCount) {
            case 2 -> 72L;
            case 3 -> 168L;
            default -> 24L;
        };
    }
}
