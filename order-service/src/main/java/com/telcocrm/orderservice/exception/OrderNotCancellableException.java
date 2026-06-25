package com.telcocrm.orderservice.exception;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import org.springframework.http.HttpStatus;


import java.util.UUID;

public class OrderNotCancellableException extends BaseException {

    public OrderNotCancellableException(UUID orderId, OrderStatus currentStatus) {
        super(
            "Order with id: " + orderId + " cannot be cancelled. Current status: " + currentStatus,
            HttpStatus.UNPROCESSABLE_CONTENT,
            "ORDER_NOT_CANCELLABLE"
        );
    }
}