package com.telcocrm.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationDispatcher dispatcher;

    @Bean
    public Consumer<Map<String, Object>> customerRegisteredEvent() {
        return event -> {
            log.info("Received CustomerRegisteredEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("CUSTOMER_REGISTERED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> customerKycApprovedEvent() {
        return event -> {
            log.info("Received CustomerKYCApprovedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("CUSTOMER_KYC_APPROVED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> customerKycRejectedEvent() {
        return event -> {
            log.info("Received CustomerKYCRejectedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("CUSTOMER_KYC_REJECTED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> customerUpdatedEvent() {
        return event -> {
            log.info("Received CustomerUpdatedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("CUSTOMER_UPDATED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> orderCreatedEvent() {
        return event -> {
            log.info("Received OrderCreatedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("ORDER_CREATED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> orderConfirmedEvent() {
        return event -> {
            log.info("Received OrderConfirmedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("ORDER_CONFIRMED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> orderCancelledEvent() {
        return event -> {
            log.info("Received OrderCancelledEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("ORDER_CANCELLED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> quotaThresholdReachedEvent() {
        return event -> {
            log.info("Received QuotaThresholdReachedEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("QUOTA_THRESHOLD_REACHED", customerId, event);
        };
    }

    @Bean
    public Consumer<Map<String, Object>> quotaExceededEvent() {
        return event -> {
            log.info("Received QuotaExceededEvent: {}", event);
            UUID customerId = parseUuid(event.get("customerId"));
            dispatcher.dispatchFromEvent("QUOTA_EXCEEDED", customerId, event);
        };
    }

    private UUID parseUuid(Object value) {
        if (value instanceof String s) return UUID.fromString(s);
        if (value instanceof UUID u) return u;
        return null;
    }
}
