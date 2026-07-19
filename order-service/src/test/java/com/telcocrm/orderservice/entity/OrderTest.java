package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var order = Order.builder()
                .id(id)
                .status(OrderStatus.PAID)
                .totalAmount(BigDecimal.TEN)
                .currency("TRY")
                .build();

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("10");
        assertThat(order.getCurrency()).isEqualTo("TRY");
    }

    @Test
    void shouldDefaultDeletedToFalse() {
        var order = Order.builder().build();

        assertThat(order.isDeleted()).isFalse();
    }

    @Test
    void shouldDefaultItemsToEmptyList() {
        var order = Order.builder().build();

        assertThat(order.getItems()).isNotNull().isEmpty();
    }
}
