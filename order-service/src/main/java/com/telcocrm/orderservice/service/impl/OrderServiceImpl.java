package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.ProductCatalogClient;
import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.exception.OrderNotCancellableException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.mapper.OrderMapper;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.service.OrderService;
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

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        // todo: Müşteri kontrolü customer service hazır olunca açılacak

        // CustomerResponse customer =
        // customerClient.getCustomerById(request.customerId());
        // if (!"ACTIVE".equals(customer.status())) {
        // throw new IllegalStateException("Customer is not active");
        // }

        List<OrderItem> items = request.items().stream()
                .map(itemRequest -> {

                    // todo: Catalog kontrolü — product-catalog-service hazır olunca açılacak
                    // ProductResponse product =
                    // productCatalogClient.getProductByCode(itemRequest.productCode());

                    // mock data
                    return OrderItem.builder()
                            .productCode(itemRequest.productCode())
                            .productName("Mock Product")
                            .productType(itemRequest.productType())
                            .quantity(itemRequest.quantity())
                            .unitPrice(BigDecimal.valueOf(100))
                            .lineTotal(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(itemRequest.quantity())))
                            .build();
                })
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        // todo: Kafka'ya OrderCreated eventi gönderilecek
        // kafkaProducer.sendOrderCreatedEvent(order);

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

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotCancellableException(orderId, order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.reason());

        SagaState sagaState = order.getSagaState();
        if (sagaState == null) {
            throw new IllegalStateException("Order with id: " + orderId + " has no associated saga state");
        }
        sagaState.setCurrentStep(SagaStep.FAILED);
        sagaState.setErrorMessage("Order cancelled by user: " + request.reason());

        // todo: Kafka'ya OrderCancelled eventi gönder
        // kafkaProducer.sendOrderCancelledEvent(order);

        orderRepository.save(order);

        return orderMapper.toResponse(order);
    }
}