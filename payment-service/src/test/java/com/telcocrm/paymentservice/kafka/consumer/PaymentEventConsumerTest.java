package com.telcocrm.paymentservice.kafka.consumer;

import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;
import com.telcocrm.paymentservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.paymentservice.service.PaymentEventProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentEventProcessingService paymentEventProcessingService;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void orderCreatedEvent_shouldDelegateToProcessingService() {
        Consumer<OrderCreatedEvent> consumer = paymentEventConsumer.orderCreatedEvent();
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), "TRY", "a@b.com", "A", "B"
        );

        consumer.accept(event);

        verify(paymentEventProcessingService).processOrderCreated(event);
    }

    @Test
    void subscriptionActivationFailedEvent_shouldDelegateToProcessingService() {
        Consumer<SubscriptionActivationFailedEvent> consumer = paymentEventConsumer.subscriptionActivationFailedEvent();
        SubscriptionActivationFailedEvent event = new SubscriptionActivationFailedEvent(
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "reason"
        );

        consumer.accept(event);

        verify(paymentEventProcessingService).processSubscriptionActivationFailed(event);
    }
}
