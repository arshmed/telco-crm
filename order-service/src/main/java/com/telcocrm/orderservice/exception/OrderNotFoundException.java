package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends BaseException {

    public OrderNotFoundException(UUID orderId) {
        super(
            "Order not found with id: " + orderId,
            HttpStatus.NOT_FOUND,
            "ORDER_NOT_FOUND"
        );
    }
}