package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivationFailedEvent;
import com.telcocrm.orderservice.exception.InvalidOrderStateException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.repository.ProcessedEventRepository;
import com.telcocrm.orderservice.rules.OrderStateRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProcessingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private com.telcocrm.orderservice.service.OutboxService outboxService;

    @Mock
    private OrderStateRules orderStateRules;

    @Mock
    private CustomerClient customerClient;

    @InjectMocks
    private OrderEventProcessingServiceImpl processingService;

    @Captor
    private ArgumentCaptor<com.telcocrm.orderservice.entity.ProcessedEvent> processedEventCaptor;

    private Order anOrder(UUID orderId, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .customerId(UUID.randomUUID())
                .status(status)
                .sagaState(SagaState.builder().currentStep(SagaStep.AWAITING_PAYMENT).build())
                .build();
    }

    // ---- processPaymentCompleted ----

    @Test
    void shouldProcessPaymentCompleted() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.PENDING_PAYMENT);
        var event = new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

        processingService.processPaymentCompleted(event);

        verify(orderStateRules).markPaymentCompleted(order, event.paymentId());
        verify(orderRepository).save(order);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertThatEventIdMatches(processedEventCaptor.getValue().getEventId(), event.eventId());
    }

    @Test
    void shouldSkipPaymentCompletedWhenAlreadyProcessed() {
        var event = new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processPaymentCompleted(event);

        verify(orderRepository, never()).findByIdAndDeletedFalse(any());
        verify(orderStateRules, never()).markPaymentCompleted(any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPaymentCompletedOrderNotFound() {
        var orderId = UUID.randomUUID();
        var event = new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingService.processPaymentCompleted(event))
                .isInstanceOf(OrderNotFoundException.class);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldSkipPaymentCompletedWhenOrderInInvalidState() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.FULFILLED);
        var event = new PaymentCompletedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOrderStateException(orderId, order.getStatus(), OrderStatus.PENDING_PAYMENT))
                .when(orderStateRules).markPaymentCompleted(order, event.paymentId());

        processingService.processPaymentCompleted(event);

        verify(orderRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    // ---- processPaymentFailed ----

    @Test
    void shouldProcessPaymentFailed() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.PENDING_PAYMENT);
        var event = new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, "insufficient funds");

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(customerClient.getCustomerById(order.getCustomerId())).thenReturn(
                new CustomerResponse(order.getCustomerId(), "ACTIVE", "test@example.com", "John", "Doe"));

        processingService.processPaymentFailed(event);

        verify(orderStateRules).markPaymentFailed(order, event.reason());
        verify(orderRepository).save(order);
        verify(outboxService).saveEvent(eq("ORDER"), any(), eq("order-cancelled-topic"), any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldSkipPaymentFailedWhenAlreadyProcessed() {
        var event = new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "reason");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processPaymentFailed(event);

        verify(orderRepository, never()).findByIdAndDeletedFalse(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenPaymentFailedOrderNotFound() {
        var orderId = UUID.randomUUID();
        var event = new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, "reason");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingService.processPaymentFailed(event))
                .isInstanceOf(OrderNotFoundException.class);
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldSkipPaymentFailedWhenOrderInInvalidState() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.FULFILLED);
        var event = new PaymentFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, "reason");

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOrderStateException(orderId, order.getStatus(), OrderStatus.PENDING_PAYMENT))
                .when(orderStateRules).markPaymentFailed(order, event.reason());

        processingService.processPaymentFailed(event);

        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    // ---- processSubscriptionActivated ----

    @Test
    void shouldProcessSubscriptionActivated() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.PAID);
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(customerClient.getCustomerById(order.getCustomerId())).thenReturn(
                new CustomerResponse(order.getCustomerId(), "ACTIVE", "test@example.com", "John", "Doe"));

        processingService.processSubscriptionActivated(event);

        verify(orderStateRules).markSubscriptionActivated(order, event.subscriptionId());
        verify(orderRepository).save(order);
        verify(outboxService).saveEvent(eq("ORDER"), any(), eq("order-confirmed-topic"), any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldSkipSubscriptionActivatedWhenAlreadyProcessed() {
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processSubscriptionActivated(event);

        verify(orderRepository, never()).findByIdAndDeletedFalse(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenSubscriptionActivatedOrderNotFound() {
        var orderId = UUID.randomUUID();
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingService.processSubscriptionActivated(event))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void shouldSkipSubscriptionActivatedWhenOrderInInvalidState() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.PENDING_PAYMENT);
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, UUID.randomUUID());

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOrderStateException(orderId, order.getStatus(), OrderStatus.PAID))
                .when(orderStateRules).markSubscriptionActivated(order, event.subscriptionId());

        processingService.processSubscriptionActivated(event);

        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    // ---- processSubscriptionActivationFailed ----

    @Test
    void shouldProcessSubscriptionActivationFailed() {
        var orderId = UUID.randomUUID();
        var order = anOrder(orderId, OrderStatus.PAID);
        var event = new SubscriptionActivationFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, "no capacity");

        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(customerClient.getCustomerById(order.getCustomerId())).thenReturn(
                new CustomerResponse(order.getCustomerId(), "ACTIVE", "test@example.com", "John", "Doe"));

        processingService.processSubscriptionActivationFailed(event);

        verify(orderStateRules).markSubscriptionActivationFailed(eq(order), contains("no capacity"));
        verify(orderRepository).save(order);
        verify(outboxService).saveEvent(eq("ORDER"), any(), eq("order-cancelled-topic"), any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldSkipSubscriptionActivationFailedWhenAlreadyProcessed() {
        var event = new SubscriptionActivationFailedEvent(UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), "reason");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processSubscriptionActivationFailed(event);

        verify(orderRepository, never()).findByIdAndDeletedFalse(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenSubscriptionActivationFailedOrderNotFound() {
        var orderId = UUID.randomUUID();
        var event = new SubscriptionActivationFailedEvent(UUID.randomUUID(), LocalDateTime.now(), orderId, "reason");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processingService.processSubscriptionActivationFailed(event))
                .isInstanceOf(OrderNotFoundException.class);
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    private void assertThatEventIdMatches(UUID actual, UUID expected) {
        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected);
    }
}
