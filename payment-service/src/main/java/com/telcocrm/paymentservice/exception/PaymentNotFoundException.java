package com.telcocrm.paymentservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException(UUID paymentId) {
        super(
            "Payment not found with id: " + paymentId,
            HttpStatus.NOT_FOUND,
            "PAYMENT_NOT_FOUND"
        );
    }
}
