package com.telcocrm.orderservice.kafka.consumer;

import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.orderservice.service.OrderEventProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

// TODO: subscription-service ile topic adı netleştirilecek
@Configuration
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventProcessingService orderEventProcessingService;

    @Bean
    public Consumer<PaymentCompletedEvent> paymentCompletedEvent() {
        return orderEventProcessingService::processPaymentCompleted;
    }

    @Bean
    public Consumer<PaymentFailedEvent> paymentFailedEvent() {
        return orderEventProcessingService::processPaymentFailed;
    }

    @Bean
    public Consumer<SubscriptionActivatedEvent> subscriptionActivatedEvent() {
        return orderEventProcessingService::processSubscriptionActivated;
    }

    @Bean
    public Consumer<SubscriptionActivationFailedEvent> subscriptionActivationFailedEvent() {
        return orderEventProcessingService::processSubscriptionActivationFailed;
    }
}
