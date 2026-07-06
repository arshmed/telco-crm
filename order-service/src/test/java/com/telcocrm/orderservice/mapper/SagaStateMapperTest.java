package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStateMapperTest {

    private final SagaStateMapper mapper = new SagaStateMapperImpl();

    @Test
    void shouldMapAllFields() {
        var lastUpdated = LocalDateTime.now();
        var sagaState = SagaState.builder()
                .currentStep(SagaStep.AWAITING_SUBSCRIPTION)
                .retryCount(2)
                .errorMessage("retrying")
                .lastUpdated(lastUpdated)
                .build();

        var response = mapper.toResponse(sagaState);

        assertThat(response.currentStep()).isEqualTo(SagaStep.AWAITING_SUBSCRIPTION);
        assertThat(response.retryCount()).isEqualTo(2);
        assertThat(response.errorMessage()).isEqualTo("retrying");
        assertThat(response.lastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    void shouldReturnNullWhenSagaStateNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
