package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.ProcessedEvent;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.PaymentRefundedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.orderservice.event.publish.OrderCancelledEvent;
import com.telcocrm.orderservice.event.publish.OrderConfirmedEvent;
import com.telcocrm.orderservice.exception.InvalidOrderStateException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.repository.ProcessedEventRepository;
import com.telcocrm.orderservice.rules.OrderStateRules;
import com.telcocrm.orderservice.service.OrderAuditService;
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
    private final OrderAuditService orderAuditService;
    private final OrderStateRules orderStateRules;
    private final CustomerClient customerClient;

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

        orderAuditService.log(order, "Payment completed: paymentId=" + event.paymentId());

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

        orderAuditService.log(order, order.getCancellationReason());

        CustomerResponse customer = customerClient.getCustomerById(order.getCustomerId());

        outboxService.saveEvent(
            "ORDER",
            order.getId().toString(),
            "order-cancelled-topic",
            OrderCancelledEvent.of(
                order.getId(),
                order.getCustomerId(),
                order.getCancellationReason(),
                customer.email(),
                customer.firstName(),
                customer.lastName()
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

        orderAuditService.log(order, "Subscription activated: subscriptionId=" + event.subscriptionId());

        CustomerResponse customer = customerClient.getCustomerById(order.getCustomerId());

        outboxService.saveEvent(
            "ORDER",
            order.getId().toString(),
            "order-confirmed-topic",
            OrderConfirmedEvent.of(
                order.getId(),
                order.getCustomerId(),
                event.subscriptionId(),
                customer.email(),
                customer.firstName(),
                customer.lastName()
            )
        );

        markProcessed(event.eventId());

        log.info("SubscriptionActivated processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processSubscriptionActivationFailed(SubscriptionActivationFailedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("SubscriptionActivationFailedEvent already processed: {}", event.eventId());
            return;
        }

        Order order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        String message = "Subscription activation failed: " + event.reason();
        orderStateRules.markSubscriptionActivationFailed(order, message);

        orderRepository.save(order);

        orderAuditService.log(order, message);

        CustomerResponse customer = customerClient.getCustomerById(order.getCustomerId());

        outboxService.saveEvent(
            "ORDER",
            order.getId().toString(),
            "order-cancelled-topic",
            OrderCancelledEvent.of(
                order.getId(),
                order.getCustomerId(),
                message,
                customer.email(),
                customer.firstName(),
                customer.lastName()
            )
        );

        markProcessed(event.eventId());

        log.info("SubscriptionActivationFailed processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processPaymentRefunded(PaymentRefundedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("PaymentRefundedEvent already processed: {}", event.eventId());
            return;
        }

        Order order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        if (order.getSagaState().getCurrentStep() != SagaStep.COMPENSATING) {
            log.warn("Skipping PaymentRefundedEvent {}: order {} saga is not COMPENSATING (currentStep={})",
                    event.eventId(), order.getId(), order.getSagaState().getCurrentStep());
            return;
        }

        orderStateRules.markPaymentRefunded(order);

        orderRepository.save(order);

        orderAuditService.log(order, "Payment refunded: paymentId=" + event.paymentId());

        markProcessed(event.eventId());

        log.info("PaymentRefunded processed for orderId: {}", event.orderId());
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
