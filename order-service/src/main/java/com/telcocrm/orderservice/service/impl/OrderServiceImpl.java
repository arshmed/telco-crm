package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.ProductCatalogClient;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.IdempotencyKey;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.event.publish.OrderCancelledEvent;
import com.telcocrm.orderservice.event.publish.OrderCreatedEvent;
import com.telcocrm.orderservice.exception.DuplicateRequestException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.mapper.OrderMapper;
import com.telcocrm.orderservice.repository.IdempotencyKeyRepository;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.rules.OrderPricingRules;
import com.telcocrm.orderservice.rules.OrderStateRules;
import com.telcocrm.orderservice.service.OrderAuditService;
import com.telcocrm.orderservice.service.OrderService;
import com.telcocrm.orderservice.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final CustomerClient customerClient;
    private final ProductCatalogClient productCatalogClient;
    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final OrderAuditService orderAuditService;
    private final OrderPricingRules orderPricingRules;
    private final OrderStateRules orderStateRules;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {

        boolean hasIdempotencyKey = idempotencyKey != null && !idempotencyKey.isBlank();

        if (hasIdempotencyKey) {
            Optional<OrderResponse> replay = idempotencyKeyRepository.findByKey(idempotencyKey)
                    .map(existing -> orderRepository.findByIdAndDeletedFalse(existing.getOrderId())
                            .orElseThrow(() -> new OrderNotFoundException(existing.getOrderId())))
                    .map(orderMapper::toResponse);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        CustomerResponse customer = customerClient.getCustomerById(request.customerId());
        if (!ACTIVE_STATUS.equals(customer.status())) {
            throw new IllegalStateException("Customer " + request.customerId() + " is not active");
        }

        List<OrderItem> items = new ArrayList<>();
        String currency = null;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductResponse product = productCatalogClient.getProductByCode(itemRequest.productType(), itemRequest.productCode());

            if (product.status() != null && !ACTIVE_STATUS.equals(product.status())) {
                throw new IllegalStateException("Product " + itemRequest.productCode() + " is not active");
            }

            if (currency == null) {
                currency = product.currency();
            } else if (!currency.equals(product.currency())) {
                throw new IllegalStateException("Order items have mixed currencies");
            }

            items.add(orderPricingRules.buildOrderItem(itemRequest, product));
        }

        BigDecimal totalAmount = orderPricingRules.calculateTotalAmount(items);

        Order order = Order.builder()
                .customerId(request.customerId())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .currency(currency)
                .build();

        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        SagaState sagaState = SagaState.builder()
                .order(order)
                .currentStep(SagaStep.AWAITING_PAYMENT)
                .build();

        order.setSagaState(sagaState);

        orderRepository.save(order);

        if (hasIdempotencyKey) {
            try {
                idempotencyKeyRepository.save(IdempotencyKey.builder()
                        .key(idempotencyKey)
                        .orderId(order.getId())
                        .createdAt(Instant.now())
                        .build());
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateRequestException(idempotencyKey);
            }
        }

        orderAuditService.log(order, "Order created");

        outboxService.saveEvent(
                "ORDER",
                order.getId().toString(),
                "order-created-topic",
                OrderCreatedEvent.of(
                        order.getId(),
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        customer.email(),
                        customer.firstName(),
                        customer.lastName()));
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
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(UUID customerId, Pageable pageable) {
        Page<Order> orders = (customerId != null)
                ? orderRepository.findByCustomerIdAndDeletedFalse(customerId, pageable)
                : orderRepository.findByDeletedFalse(pageable);

        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request) {

        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        orderStateRules.cancel(order, request.reason());

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
                        customer.lastName()));

        return orderMapper.toResponse(order);
    }
}