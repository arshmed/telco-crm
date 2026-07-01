package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.ProcessedEvent;
import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.publish.OrderCancelledEvent;
import com.telcocrm.orderservice.event.publish.OrderConfirmedEvent;
import com.telcocrm.orderservice.exception.InvalidOrderStateException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.repository.ProcessedEventRepository;
import com.telcocrm.orderservice.rules.OrderStateRules;
import com.telcocrm.orderservice.service.OrderEventProcessingService;
import com.telcocrm.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessingServiceImpl implements OrderEventProcessingService {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    private final OrderStateRules orderStateRules;

    @Override
    @Transactional
    public void processPaymentCompleted(PaymentCompletedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("PaymentCompletedEvent already processed: {}", event.eventId());
            return;
        }

        Order order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        try {
            orderStateRules.markPaymentCompleted(order, event.paymentId());
        } catch (InvalidOrderStateException ex) {
            log.warn("Skipping PaymentCompletedEvent {}: {}", event.eventId(), ex.getMessage());
            return;
        }

        orderRepository.save(order);

        markProcessed(event.eventId());

        log.info("PaymentCompleted processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processPaymentFailed(PaymentFailedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("PaymentFailedEvent already processed: {}", event.eventId());
            return;
        }

        Order order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        try {
            orderStateRules.markPaymentFailed(order, event.reason());
        } catch (InvalidOrderStateException ex) {
            log.warn("Skipping PaymentFailedEvent {}: {}", event.eventId(), ex.getMessage());
            return;
        }

        orderRepository.save(order);

        outboxService.saveEvent(
            "ORDER",
            order.getId().toString(),
            "orderCancelledEvent",
            OrderCancelledEvent.of(
                order.getId(),
                order.getCustomerId(),
                order.getCancellationReason()
            )
        );

        markProcessed(event.eventId());

        log.info("PaymentFailed processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processSubscriptionActivated(SubscriptionActivatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("SubscriptionActivatedEvent already processed: {}", event.eventId());
            return;
        }

        Order order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        try {
            orderStateRules.markSubscriptionActivated(order, event.subscriptionId());
        } catch (InvalidOrderStateException ex) {
            log.warn("Skipping SubscriptionActivatedEvent {}: {}", event.eventId(), ex.getMessage());
            return;
        }

        orderRepository.save(order);

        outboxService.saveEvent(
            "ORDER",
            order.getId().toString(),
            "orderConfirmedEvent",
            OrderConfirmedEvent.of(
                order.getId(),
                order.getCustomerId(),
                event.subscriptionId()
            )
        );

        markProcessed(event.eventId());

        log.info("SubscriptionActivated processed for orderId: {}", event.orderId());
    }

    private void markProcessed(UUID eventId) {
        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build()
        );
    }
}
