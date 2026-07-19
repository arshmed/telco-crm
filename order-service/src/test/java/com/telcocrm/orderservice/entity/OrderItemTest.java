package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.OrderItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var order = Order.builder().id(UUID.randomUUID()).build();

        var item = OrderItem.builder()
                .id(id)
                .order(order)
                .productCode("TARIFF-1")
                .productName("Tariff One")
                .productType(OrderItemType.TARIFF)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(20))
                .build();

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getOrder()).isEqualTo(order);
        assertThat(item.getProductCode()).isEqualTo("TARIFF-1");
        assertThat(item.getProductName()).isEqualTo("Tariff One");
        assertThat(item.getProductType()).isEqualTo(OrderItemType.TARIFF);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10");
        assertThat(item.getLineTotal()).isEqualByComparingTo("20");
    }
}
