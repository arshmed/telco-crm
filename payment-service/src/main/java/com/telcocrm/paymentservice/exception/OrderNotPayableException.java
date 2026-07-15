package com.telcocrm.paymentservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotPayableException extends BaseException {

    public OrderNotPayableException(UUID orderId, String orderStatus) {
        super(
            "Order with id: " + orderId + " is not payable in its current status: " + orderStatus,
            HttpStatus.CONFLICT,
            "ORDER_NOT_PAYABLE"
        );
    }
}
