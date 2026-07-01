package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String serviceName, String errorCode, Throwable cause) {
        super(
            serviceName + " is currently unavailable. Please try again later.",
            cause,
            HttpStatus.SERVICE_UNAVAILABLE,
            errorCode
        );
    }
}
