package com.telcocrm.orderservice.kafka.consumer;

import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.orderservice.service.OrderEventProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderEventProcessingService orderEventProcessingService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Test
    void paymentCompletedEvent_shouldDelegateToProcessingService() {
        var event = new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID());

        orderEventConsumer.paymentCompletedEvent().accept(event);

        verify(orderEventProcessingService).processPaymentCompleted(event);
        verifyNoMoreInteractions(orderEventProcessingService);
    }

    @Test
    void paymentFailedEvent_shouldDelegateToProcessingService() {
        var event = new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "insufficient funds");

        orderEventConsumer.paymentFailedEvent().accept(event);

        verify(orderEventProcessingService).processPaymentFailed(event);
        verifyNoMoreInteractions(orderEventProcessingService);
    }

    @Test
    void subscriptionActivatedEvent_shouldDelegateToProcessingService() {
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID());

        orderEventConsumer.subscriptionActivatedEvent().accept(event);

        verify(orderEventProcessingService).processSubscriptionActivated(event);
        verifyNoMoreInteractions(orderEventProcessingService);
    }

    @Test
    void subscriptionActivationFailedEvent_shouldDelegateToProcessingService() {
        var event = new SubscriptionActivationFailedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "no capacity");

        orderEventConsumer.subscriptionActivationFailedEvent().accept(event);

        verify(orderEventProcessingService).processSubscriptionActivationFailed(event);
        verifyNoMoreInteractions(orderEventProcessingService);
    }
}
