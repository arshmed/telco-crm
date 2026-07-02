package com.telcocrm.orderservice.rules;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderPricingRules {

    public OrderItem buildOrderItem(OrderItemRequest request, ProductResponse product) {
        BigDecimal unitPrice = product.price();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        return OrderItem.builder()
                .productCode(request.productCode())
                .productName(product.name())
                .productType(request.productType())
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }

    public BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
