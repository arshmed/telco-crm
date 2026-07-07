package com.telcocrm.orderservice.service.impl;

import com.telcocrm.orderservice.client.CustomerClient;
import com.telcocrm.orderservice.client.ProductCatalogClient;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.mapper.OrderMapper;
import com.telcocrm.orderservice.repository.OrderRepository;
import com.telcocrm.orderservice.rules.OrderPricingRules;
import com.telcocrm.orderservice.rules.OrderStateRules;
import com.telcocrm.orderservice.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private OrderPricingRules orderPricingRules;

    @Mock
    private OrderStateRules orderStateRules;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private CreateOrderRequest validRequest() {
        return new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 1))
        );
    }

    private CustomerResponse activeCustomer(UUID id) {
        return new CustomerResponse(id, "ACTIVE", "test@email.com", "Test", "User");
    }

    private ProductResponse activeProduct() {
        return new ProductResponse("TARIFF-1", "Tariff One", BigDecimal.TEN, "ACTIVE", "TRY");
    }

    private OrderItem anOrderItem() {
        return OrderItem.builder()
                .productCode("TARIFF-1")
                .productName("Tariff One")
                .productType(OrderItemType.TARIFF)
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.TEN)
                .build();
    }

    @Test
    void shouldCreateOrder() {
        var request = validRequest();
        var customer = activeCustomer(request.customerId());
        var product = activeProduct();
        var orderItem = anOrderItem();

        when(customerClient.getCustomerById(request.customerId())).thenReturn(customer);
        when(productCatalogClient.getProductByCode(OrderItemType.TARIFF, "TARIFF-1")).thenReturn(product);
        when(orderPricingRules.buildOrderItem(request.items().getFirst(), product)).thenReturn(orderItem);
        when(orderPricingRules.calculateTotalAmount(anyList())).thenReturn(BigDecimal.TEN);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponse.class));

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(request.customerId());
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(saved.getTotalAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(saved.getCurrency()).isEqualTo("TRY");
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getSagaState()).isNotNull();
        assertThat(saved.getSagaState().getCurrentStep()).isEqualTo(SagaStep.AWAITING_PAYMENT);
        verify(outboxService).saveEvent(eq("ORDER"), any(), eq("order-created-topic"), any());
    }

    @Test
    void shouldThrowWhenCustomerNotActive() {
        var request = validRequest();
        var customer = new CustomerResponse(request.customerId(), "PENDING", "test@email.com", "Test", "User");

        when(customerClient.getCustomerById(request.customerId())).thenReturn(customer);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not active");
        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldThrowWhenProductNotActive() {
        var request = validRequest();
        var customer = activeCustomer(request.customerId());
        var inactiveProduct = new ProductResponse("TARIFF-1", "Tariff One", BigDecimal.TEN, "INACTIVE", "TRY");

        when(customerClient.getCustomerById(request.customerId())).thenReturn(customer);
        when(productCatalogClient.getProductByCode(OrderItemType.TARIFF, "TARIFF-1")).thenReturn(inactiveProduct);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not active");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenItemsHaveMixedCurrencies() {
        var request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(
                        new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 1),
                        new OrderItemRequest("ADDON-1", OrderItemType.ADDON, 1)
                )
        );
        var customer = activeCustomer(request.customerId());
        var tryProduct = new ProductResponse("TARIFF-1", "Tariff One", BigDecimal.TEN, "ACTIVE", "TRY");
        var usdProduct = new ProductResponse("ADDON-1", "Addon One", BigDecimal.ONE, "ACTIVE", "USD");

        when(customerClient.getCustomerById(request.customerId())).thenReturn(customer);
        when(productCatalogClient.getProductByCode(OrderItemType.TARIFF, "TARIFF-1")).thenReturn(tryProduct);
        when(productCatalogClient.getProductByCode(OrderItemType.ADDON, "ADDON-1")).thenReturn(usdProduct);
        when(orderPricingRules.buildOrderItem(request.items().getFirst(), tryProduct)).thenReturn(anOrderItem());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mixed currencies");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldGetOrderById() {
        var orderId = UUID.randomUUID();
        var order = Order.builder().id(orderId).build();
        var response = mock(OrderResponse.class);

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderService.getOrderById(orderId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenOrderNotFoundById() {
        var orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void shouldListOrdersByCustomerId() {
        var customerId = UUID.randomUUID();
        var order = Order.builder().id(UUID.randomUUID()).build();
        var response = mock(OrderResponse.class);
        var page = new PageImpl<>(List.of(order));

        when(orderRepository.findByCustomerIdAndDeletedFalse(eq(customerId), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(response);

        Page<OrderResponse> result = orderService.listOrders(customerId, Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(response);
        verify(orderRepository, never()).findByDeletedFalse(any());
    }

    @Test
    void shouldListAllOrdersWhenCustomerIdNull() {
        var order = Order.builder().id(UUID.randomUUID()).build();
        var response = mock(OrderResponse.class);
        var page = new PageImpl<>(List.of(order));

        when(orderRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(response);

        Page<OrderResponse> result = orderService.listOrders(null, Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(response);
        verify(orderRepository, never()).findByCustomerIdAndDeletedFalse(any(), any());
    }

    @Test
    void shouldCancelOrder() {
        var orderId = UUID.randomUUID();
        var order = Order.builder()
                .id(orderId)
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PENDING_PAYMENT)
                .sagaState(SagaState.builder().currentStep(SagaStep.AWAITING_PAYMENT).build())
                .build();
        var request = new CancelOrderRequest("customer changed mind");
        var response = mock(OrderResponse.class);

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        doAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setStatus(OrderStatus.CANCELLED);
            o.setCancellationReason("Order cancelled by user: customer changed mind");
            return null;
        }).when(orderStateRules).cancel(order, request.reason());
        when(customerClient.getCustomerById(order.getCustomerId())).thenReturn(
                new CustomerResponse(order.getCustomerId(), "ACTIVE", "test@email.com", "Test", "User"));
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderService.cancelOrder(orderId, request);

        assertThat(result).isEqualTo(response);
        verify(orderStateRules).cancel(order, request.reason());
        verify(orderRepository).save(order);
        verify(outboxService).saveEvent(eq("ORDER"), any(), eq("order-cancelled-topic"), any());
    }

    @Test
    void shouldThrowWhenCancellingNonExistentOrder() {
        var orderId = UUID.randomUUID();
        var request = new CancelOrderRequest("reason");
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, request))
                .isInstanceOf(OrderNotFoundException.class);
        verify(orderStateRules, never()).cancel(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldPropagateExceptionWhenOrderNotCancellable() {
        var orderId = UUID.randomUUID();
        var order = Order.builder().id(orderId).status(OrderStatus.FULFILLED).build();
        var request = new CancelOrderRequest("reason");

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("not cancellable")).when(orderStateRules).cancel(order, request.reason());

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, request))
                .isInstanceOf(IllegalStateException.class);
        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }
}
