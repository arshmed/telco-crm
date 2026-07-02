package com.telcocrm.orderservice.service;

import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(UUID orderId);

    OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request);

    Page<OrderResponse> listOrders(UUID customerId, Pageable pageable);
}