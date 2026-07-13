package com.telcocrm.paymentservice.exception;

import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import org.springframework.http.HttpStatus;
import java.util.UUID;

public class InvalidPaymentStateException extends BaseException {

    public InvalidPaymentStateException(UUID paymentId, PaymentStatus currentStatus, PaymentStatus expectedStatus) {
        super(
            "Payment with id: " + paymentId + " is in invalid state. Current: " + currentStatus + ", Expected: " + expectedStatus,
            HttpStatus.CONFLICT,
            "INVALID_PAYMENT_STATE"
        );
    }
}
