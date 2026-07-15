package com.telcocrm.subscriptionservice.handler;

import com.telcocrm.subscriptionservice.entity.ProcessedEvent;
import com.telcocrm.subscriptionservice.repository.ProcessedEventRepository;
import com.telcocrm.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final SubscriptionService subscriptionService;

    @Bean
    public Consumer<Map<String, Object>> orderCreatedEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing OrderCreatedEvent: {}", eventId);

            UUID orderId = UUID.fromString((String) event.get("orderId"));
            UUID customerId = UUID.fromString((String) event.get("customerId"));
            String tariffCode = (String) event.get("tariffCode");

            subscriptionService.createSubscriptionFromOrder(orderId, customerId, tariffCode);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());

            log.info("Subscription created from order {}", orderId);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> paymentCompletedEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing PaymentCompletedEvent: {}", eventId);

            UUID orderId = UUID.fromString((String) event.get("orderId"));
            subscriptionService.activateSubscriptionFromPayment(orderId);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());

            log.info("Subscription activation attempted for order {}", orderId);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> orderCancelledEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing OrderCancelledEvent: {}", eventId);

            UUID orderId = UUID.fromString((String) event.get("orderId"));
            subscriptionService.handleOrderCancelled(orderId);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());

            log.info("Order cancellation handled for order {}", orderId);
        };
    }
}
