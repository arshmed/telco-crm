package com.telcocrm.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends BaseException {

    public DuplicateRequestException(String paymentRequestId) {
        super(
            "Request with paymentRequestId: " + paymentRequestId + " is already being processed, please retry",
            HttpStatus.CONFLICT,
            "DUPLICATE_REQUEST"
        );
    }
}
