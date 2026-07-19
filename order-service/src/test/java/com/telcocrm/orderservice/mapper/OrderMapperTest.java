package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.dto.response.OrderItemResponse;
import com.telcocrm.orderservice.dto.response.SagaStateResponse;
import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMapperTest {

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private SagaStateMapper sagaStateMapper;

    @InjectMocks
    private OrderMapperImpl orderMapper;

    @Test
    void shouldMapAllFields() {
        var orderItem = OrderItem.builder().productCode("TARIFF-1").build();
        var sagaState = SagaState.builder().currentStep(SagaStep.AWAITING_PAYMENT).build();
        var order = Order.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.TEN)
                .currency("TRY")
                .paymentId(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .cancellationReason(null)
                .items(List.of(orderItem))
                .sagaState(sagaState)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var itemResponse = new OrderItemResponse(UUID.randomUUID(), "TARIFF-1", "Tariff One", OrderItemType.TARIFF, 1, BigDecimal.TEN, BigDecimal.TEN);
        var sagaResponse = new SagaStateResponse(SagaStep.AWAITING_PAYMENT, 0, null, LocalDateTime.now());
        when(orderItemMapper.toResponse(orderItem)).thenReturn(itemResponse);
        when(sagaStateMapper.toResponse(sagaState)).thenReturn(sagaResponse);

        var response = orderMapper.toResponse(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.customerId()).isEqualTo(order.getCustomerId());
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.totalAmount()).isEqualByComparingTo("10");
        assertThat(response.currency()).isEqualTo("TRY");
        assertThat(response.paymentId()).isEqualTo(order.getPaymentId());
        assertThat(response.subscriptionId()).isEqualTo(order.getSubscriptionId());
        assertThat(response.items()).containsExactly(itemResponse);
        assertThat(response.sagaState()).isEqualTo(sagaResponse);
    }

    @Test
    void shouldReturnNullWhenOrderNull() {
        assertThat(orderMapper.toResponse(null)).isNull();
    }
}
