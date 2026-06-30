package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.entity.ProcessedEvent;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.publish.OrderCancelledEvent;
import com.telcocrm.orderservice.event.publish.OrderConfirmedEvent;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.repository.ProcessedEventRepository;
import com.telcocrm.orderservice.service.OrderEventProcessingService;
import com.telcocrm.orderservice.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessingServiceImpl implements OrderEventProcessingService {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public void processPaymentCompleted(PaymentCompletedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("PaymentCompletedEvent already processed: {}", event.eventId());
            return;
        }

        var order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(event.paymentId());

        order.getSagaState().setCurrentStep(SagaStep.AWAITING_SUBSCRIPTION);

        orderRepository.save(order);

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("PaymentCompleted processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processPaymentFailed(PaymentFailedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("PaymentFailedEvent already processed: {}", event.eventId());
            return;
        }

        var order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason("Payment failed: " + event.reason());

        order.getSagaState().setCurrentStep(SagaStep.FAILED);
        order.getSagaState().setErrorMessage("Payment failed: " + event.reason());

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

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("PaymentFailed processed for orderId: {}", event.orderId());
    }

    @Override
    @Transactional
    public void processSubscriptionActivated(SubscriptionActivatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("SubscriptionActivatedEvent already processed: {}", event.eventId());
            return;
        }

        var order = orderRepository.findByIdAndDeletedFalse(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        order.setStatus(OrderStatus.FULFILLED);
        order.setSubscriptionId(event.subscriptionId());

        order.getSagaState().setCurrentStep(SagaStep.COMPLETED);

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

        processedEventRepository.save(
            ProcessedEvent.builder()
                .eventId(event.eventId())
                .processedAt(Instant.now())
                .build()
        );

        log.info("SubscriptionActivated processed for orderId: {}", event.orderId());
    }
}
