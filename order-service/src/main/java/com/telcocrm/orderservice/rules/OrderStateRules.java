package com.telcocrm.orderservice.rules;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.exception.InvalidOrderStateException;
import com.telcocrm.orderservice.exception.OrderNotCancellableException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderStateRules {

    public void cancel(Order order, String reason) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotCancellableException(order.getId(), order.getStatus());
        }

        String message = "Order cancelled by user: " + reason;
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(message);

        SagaState sagaState = requireSagaState(order);
        sagaState.setCurrentStep(SagaStep.FAILED);
        sagaState.setErrorMessage(message);
    }

    public void markPaymentCompleted(Order order, UUID paymentId) {
        requireStatus(order, OrderStatus.PENDING_PAYMENT);

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentId);
        requireSagaState(order).setCurrentStep(SagaStep.AWAITING_SUBSCRIPTION);
    }

    public void markPaymentFailed(Order order, String reason) {
        requireStatus(order, OrderStatus.PENDING_PAYMENT);

        String message = "Payment failed: " + reason;
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(message);

        SagaState sagaState = requireSagaState(order);
        sagaState.setCurrentStep(SagaStep.FAILED);
        sagaState.setErrorMessage(message);
    }

    public void markSubscriptionActivated(Order order, UUID subscriptionId) {
        requireStatus(order, OrderStatus.PAID);

        order.setStatus(OrderStatus.FULFILLED);
        order.setSubscriptionId(subscriptionId);
        requireSagaState(order).setCurrentStep(SagaStep.COMPLETED);
    }

    public void markSubscriptionActivationFailed(Order order, String errorMessage) {
        SagaState sagaState = requireSagaState(order);
        sagaState.setCurrentStep(SagaStep.COMPENSATING);
        sagaState.setErrorMessage(errorMessage);
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(order.getId(), order.getStatus(), expected);
        }
    }

    private SagaState requireSagaState(Order order) {
        SagaState sagaState = order.getSagaState();
        if (sagaState == null) {
            throw new IllegalStateException("Order with id: " + order.getId() + " has no associated saga state");
        }
        return sagaState;
    }
}
