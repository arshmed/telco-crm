package com.telcocrm.paymentservice.mapper;

import com.telcocrm.paymentservice.dto.response.PaymentAttemptResponse;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAttemptMapperTest {

    private final PaymentAttemptMapper mapper = new PaymentAttemptMapperImpl();

    @Test
    void toResponse_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        Instant attemptedAt = Instant.now();
        PaymentAttempt attempt = PaymentAttempt.builder()
                .id(id)
                .attemptNo(2)
                .response("Card declined")
                .attemptedAt(attemptedAt)
                .build();

        PaymentAttemptResponse response = mapper.toResponse(attempt);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.attemptNo()).isEqualTo(2);
        assertThat(response.response()).isEqualTo("Card declined");
        assertThat(response.attemptedAt()).isEqualTo(attemptedAt);
    }

    @Test
    void toResponse_shouldReturnNullForNullInput() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
