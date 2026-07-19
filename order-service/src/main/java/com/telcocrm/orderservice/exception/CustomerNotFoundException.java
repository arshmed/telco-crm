package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CustomerNotFoundException extends BaseException {

    public CustomerNotFoundException(UUID customerId) {
        super(
            "Customer not found with id: " + customerId,
            HttpStatus.NOT_FOUND,
            "CUSTOMER_NOT_FOUND"
        );
    }
}
