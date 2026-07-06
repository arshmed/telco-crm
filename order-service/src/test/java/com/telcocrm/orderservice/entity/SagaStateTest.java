package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.SagaStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStateTest {

    @Test
    void shouldBuildWithAllFields() {
        var sagaState = SagaState.builder()
                .currentStep(SagaStep.COMPLETED)
                .errorMessage("done")
                .build();

        assertThat(sagaState.getCurrentStep()).isEqualTo(SagaStep.COMPLETED);
        assertThat(sagaState.getErrorMessage()).isEqualTo("done");
    }

    @Test
    void shouldDefaultRetryCountToZero() {
        var sagaState = SagaState.builder().build();

        assertThat(sagaState.getRetryCount()).isZero();
    }
}
