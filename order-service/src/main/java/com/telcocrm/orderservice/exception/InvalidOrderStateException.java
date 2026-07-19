package com.telcocrm.orderservice.exception;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import org.springframework.http.HttpStatus;
import java.util.UUID;

public class InvalidOrderStateException extends BaseException {

    public InvalidOrderStateException(UUID orderId, OrderStatus currentStatus, OrderStatus expectedStatus) {
        super(
            "Order with id: " + orderId + " is in invalid state. Current: " + currentStatus + ", Expected: " + expectedStatus,
            HttpStatus.CONFLICT,
            "INVALID_ORDER_STATE"
        );
    }
}