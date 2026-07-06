package com.telcocrm.usageservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String serviceName, String errorCode, Throwable cause) {
        super(serviceName + " is unavailable", HttpStatus.SERVICE_UNAVAILABLE, errorCode);
        initCause(cause);
    }
}
