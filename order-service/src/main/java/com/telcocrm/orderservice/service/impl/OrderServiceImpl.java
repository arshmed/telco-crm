package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.ProductCatalogClient;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.event.publish.OrderCancelledEvent;
import com.telcocrm.orderservice.event.publish.OrderCreatedEvent;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.mapper.OrderMapper;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.rules.OrderPricingRules;
import com.telcocrm.orderservice.rules.OrderStateRules;
import com.telcocrm.orderservice.service.OrderService;
import com.telcocrm.orderservice.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductCatalogClient productCatalogClient;
    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final OrderPricingRules orderPricingRules;
    private final OrderStateRules orderStateRules;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        CustomerResponse customer = customerClient.getCustomerById(request.customerId());
        if (!"ACTIVE".equals(customer.status())) {
            throw new IllegalStateException("Customer " + request.customerId() + " is not active");
        }

        // todo: Catalog kontrolü — product-catalog-service hazır olunca açılacak
        // ProductResponse product = productCatalogClient.getProductByCode(itemRequest.productCode());

        List<OrderItem> items = request.items().stream()
                .map(orderPricingRules::buildOrderItem)
                .toList();

        BigDecimal totalAmount = orderPricingRules.calculateTotalAmount(items);

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .currency("TRY")
                .build();

        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        SagaState sagaState = SagaState.builder()
                .order(order)
                .currentStep(SagaStep.AWAITING_PAYMENT)
                .build();

        order.setSagaState(sagaState);

        orderRepository.save(order);

        outboxService.saveEvent(
                "ORDER",
                order.getId().toString(),
                "order-created-topic",
                OrderCreatedEvent.of(
                        order.getId(),
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        order.getCurrency()));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request) {

        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        orderStateRules.cancel(order, request.reason());

        orderRepository.save(order);

        outboxService.saveEvent(
                "ORDER",
                order.getId().toString(),
                "order-cancelled-topic",
                OrderCancelledEvent.of(
                        order.getId(),
                        order.getCustomerId(),
                        order.getCancellationReason()));

        orderRepository.flush();

        return orderMapper.toResponse(order);
    }
}