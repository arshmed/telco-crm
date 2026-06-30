package com.telcocrm.orderservice.rules;

import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderPricingRules {

    // todo: product-catalog-service hazır olunca gerçek fiyatlandırmaya bağlanacak
    private static final BigDecimal MOCK_UNIT_PRICE = BigDecimal.valueOf(100);

    public OrderItem buildOrderItem(OrderItemRequest request) {
        BigDecimal lineTotal = MOCK_UNIT_PRICE.multiply(BigDecimal.valueOf(request.quantity()));

        return OrderItem.builder()
                .productCode(request.productCode())
                .productName("Mock Product")
                .productType(request.productType())
                .quantity(request.quantity())
                .unitPrice(MOCK_UNIT_PRICE)
                .lineTotal(lineTotal)
                .build();
    }

    public BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
