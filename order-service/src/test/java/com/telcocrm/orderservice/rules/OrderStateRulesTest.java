package com.telcocrm.orderservice.rules;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.exception.InvalidOrderStateException;
import com.telcocrm.orderservice.exception.OrderNotCancellableException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateRulesTest {

    private final OrderStateRules stateRules = new OrderStateRules();

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .status(status)
                .sagaState(SagaState.builder().currentStep(SagaStep.AWAITING_PAYMENT).build())
                .build();
    }

    // ---- cancel ----

    @Test
    void shouldCancelPendingPaymentOrder() {
        var order = orderWithStatus(OrderStatus.PENDING_PAYMENT);

        stateRules.cancel(order, "customer changed mind");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).contains("customer changed mind");
        assertThat(order.getSagaState().getCurrentStep()).isEqualTo(SagaStep.FAILED);
        assertThat(order.getSagaState().getErrorMessage()).contains("customer changed mind");
    }

    @Test
    void shouldThrowWhenCancellingNonPendingOrder() {
        var order = orderWithStatus(OrderStatus.FULFILLED);

        assertThatThrownBy(() -> stateRules.cancel(order, "reason"))
                .isInstanceOf(OrderNotCancellableException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
    }

    // ---- markPaymentCompleted ----

    @Test
    void shouldMarkPaymentCompleted() {
        var order = orderWithStatus(OrderStatus.PENDING_PAYMENT);
        var paymentId = UUID.randomUUID();

        stateRules.markPaymentCompleted(order, paymentId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentId()).isEqualTo(paymentId);
        assertThat(order.getSagaState().getCurrentStep()).isEqualTo(SagaStep.AWAITING_SUBSCRIPTION);
    }

    @Test
    void shouldThrowWhenMarkingPaymentCompletedOnWrongState() {
        var order = orderWithStatus(OrderStatus.PAID);

        assertThatThrownBy(() -> stateRules.markPaymentCompleted(order, UUID.randomUUID()))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    // ---- markPaymentFailed ----

    @Test
    void shouldMarkPaymentFailed() {
        var order = orderWithStatus(OrderStatus.PENDING_PAYMENT);

        stateRules.markPaymentFailed(order, "insufficient funds");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).contains("insufficient funds");
        assertThat(order.getSagaState().getCurrentStep()).isEqualTo(SagaStep.FAILED);
    }

    @Test
    void shouldThrowWhenMarkingPaymentFailedOnWrongState() {
        var order = orderWithStatus(OrderStatus.FULFILLED);

        assertThatThrownBy(() -> stateRules.markPaymentFailed(order, "reason"))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    // ---- markSubscriptionActivated ----

    @Test
    void shouldMarkSubscriptionActivated() {
        var order = orderWithStatus(OrderStatus.PAID);
        var subscriptionId = UUID.randomUUID();

        stateRules.markSubscriptionActivated(order, subscriptionId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(order.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(order.getSagaState().getCurrentStep()).isEqualTo(SagaStep.COMPLETED);
    }

    @Test
    void shouldThrowWhenMarkingSubscriptionActivatedOnWrongState() {
        var order = orderWithStatus(OrderStatus.PENDING_PAYMENT);

        assertThatThrownBy(() -> stateRules.markSubscriptionActivated(order, UUID.randomUUID()))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    // ---- markSubscriptionActivationFailed ----

    @Test
    void shouldMarkSubscriptionActivationFailed() {
        var order = orderWithStatus(OrderStatus.PAID);

        stateRules.markSubscriptionActivationFailed(order, "no capacity");

        assertThat(order.getSagaState().getCurrentStep()).isEqualTo(SagaStep.COMPENSATING);
        assertThat(order.getSagaState().getErrorMessage()).isEqualTo("no capacity");
    }

    @Test
    void shouldThrowWhenNoSagaStateAssociated() {
        var order = Order.builder().id(UUID.randomUUID()).status(OrderStatus.PAID).build();

        assertThatThrownBy(() -> stateRules.markSubscriptionActivationFailed(order, "no capacity"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no associated saga state");
    }
}
