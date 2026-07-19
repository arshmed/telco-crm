package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemMapperTest {

    private final OrderItemMapper mapper = new OrderItemMapperImpl();

    @Test
    void shouldMapAllFields() {
        var item = OrderItem.builder()
                .id(UUID.randomUUID())
                .productCode("TARIFF-1")
                .productName("Tariff One")
                .productType(OrderItemType.TARIFF)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(20))
                .build();

        var response = mapper.toResponse(item);

        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.productCode()).isEqualTo("TARIFF-1");
        assertThat(response.productName()).isEqualTo("Tariff One");
        assertThat(response.productType()).isEqualTo(OrderItemType.TARIFF);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.unitPrice()).isEqualByComparingTo("10");
        assertThat(response.lineTotal()).isEqualByComparingTo("20");
    }

    @Test
    void shouldReturnNullWhenOrderItemNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
