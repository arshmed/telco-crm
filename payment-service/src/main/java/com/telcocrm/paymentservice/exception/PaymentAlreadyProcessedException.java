package com.telcocrm.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyProcessedException extends BaseException {

    public PaymentAlreadyProcessedException(String paymentRequestId) {
        super(
            "Payment request with id: " + paymentRequestId + " is already being processed, please retry",
            HttpStatus.CONFLICT,
            "PAYMENT_ALREADY_PROCESSED"
        );
    }
}
