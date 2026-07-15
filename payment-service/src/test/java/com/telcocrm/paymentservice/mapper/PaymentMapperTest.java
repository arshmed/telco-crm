package com.telcocrm.paymentservice.mapper;

import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        PaymentMapperImpl impl = new PaymentMapperImpl();
        PaymentAttemptMapperImpl attemptMapper = new PaymentAttemptMapperImpl();
        ReflectionTestUtils.setField(impl, "paymentAttemptMapper", attemptMapper);
        paymentMapper = impl;
    }

    @Test
    void toResponse_shouldMapAllFields() {
        UUID paymentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        PaymentAttempt attempt = PaymentAttempt.builder()
                .id(attemptId)
                .attemptNo(1)
                .response("MOCK_PSP_APPROVED")
                .attemptedAt(Instant.now())
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("199.99"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .externalRef("MOCK-REF-abc")
                .failureReason(null)
                .paidAt(Instant.now())
                .attempts(List.of(attempt))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(response.currency()).isEqualTo("TRY");
        assertThat(response.method()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.externalRef()).isEqualTo("MOCK-REF-abc");
        assertThat(response.attempts()).hasSize(1);
        assertThat(response.attempts().get(0).attemptNo()).isEqualTo(1);
    }

    @Test
    void toResponse_shouldHandleNullAttempts() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .currency("TRY")
                .method(PaymentMethod.WALLET)
                .status(PaymentStatus.PENDING)
                .build();

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    }
}
