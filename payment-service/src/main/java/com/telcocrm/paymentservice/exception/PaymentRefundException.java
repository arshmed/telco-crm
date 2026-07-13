package com.telcocrm.paymentservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentRefundException extends BaseException {

    public PaymentRefundException(UUID paymentId, String reason) {
        super(
            "Refund failed for payment with id: " + paymentId + ": " + reason,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PAYMENT_REFUND_FAILED"
        );
    }
}
