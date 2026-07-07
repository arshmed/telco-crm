package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends BaseException {

    public DuplicateRequestException(String idempotencyKey) {
        super(
            "Request with Idempotency-Key: " + idempotencyKey + " is already being processed, please retry",
            HttpStatus.CONFLICT,
            "DUPLICATE_REQUEST"
        );
    }
}
