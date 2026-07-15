package com.telcocrm.usageservice.exception;

import org.springframework.http.HttpStatus;

public class DownstreamAccessException extends BaseException {

    public DownstreamAccessException(String serviceName, String reason, Throwable cause) {
        super(
            serviceName + " access denied: " + reason,
            cause,
            HttpStatus.BAD_GATEWAY,
            serviceName.toUpperCase().replace(" ", "_") + "_ACCESS_DENIED"
        );
    }
}
