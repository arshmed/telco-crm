package com.telcocrm.orderservice.rules;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.entity.OrderItem;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPricingRulesTest {

    private final OrderPricingRules pricingRules = new OrderPricingRules();

    @Test
    void shouldBuildOrderItemFromProductAndRequest() {
        var request = new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 3);
        var product = new ProductResponse("TARIFF-1", "Tariff One", BigDecimal.valueOf(10), "ACTIVE", "TRY");

        OrderItem item = pricingRules.buildOrderItem(request, product);

        assertThat(item.getProductCode()).isEqualTo("TARIFF-1");
        assertThat(item.getProductName()).isEqualTo("Tariff One");
        assertThat(item.getProductType()).isEqualTo(OrderItemType.TARIFF);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10");
        assertThat(item.getLineTotal()).isEqualByComparingTo("30");
    }

    @Test
    void shouldBuildOrderItemWithZeroQuantityLineTotal() {
        var request = new OrderItemRequest("ADDON-1", OrderItemType.ADDON, 0);
        var product = new ProductResponse("ADDON-1", "Addon One", BigDecimal.valueOf(5), "ACTIVE", "TRY");

        OrderItem item = pricingRules.buildOrderItem(request, product);

        assertThat(item.getLineTotal()).isEqualByComparingTo("0");
    }

    @Test
    void shouldCalculateTotalAmountAcrossItems() {
        var item1 = OrderItem.builder().lineTotal(BigDecimal.valueOf(10)).build();
        var item2 = OrderItem.builder().lineTotal(BigDecimal.valueOf(25)).build();

        BigDecimal total = pricingRules.calculateTotalAmount(List.of(item1, item2));

        assertThat(total).isEqualByComparingTo("35");
    }

    @Test
    void shouldReturnZeroWhenNoItems() {
        BigDecimal total = pricingRules.calculateTotalAmount(Collections.emptyList());

        assertThat(total).isEqualByComparingTo("0");
    }
}
