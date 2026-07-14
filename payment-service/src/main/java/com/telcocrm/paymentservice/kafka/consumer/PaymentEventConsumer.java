package com.telcocrm.paymentservice.kafka.consumer;

import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;
import com.telcocrm.paymentservice.service.PaymentEventProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentEventProcessingService paymentEventProcessingService;

    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedEvent() {
        return paymentEventProcessingService::processOrderCreated;
    }
}
