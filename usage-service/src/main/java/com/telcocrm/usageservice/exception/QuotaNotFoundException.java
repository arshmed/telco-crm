package com.telcocrm.usageservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class QuotaNotFoundException extends BaseException {

    public QuotaNotFoundException(UUID subscriptionId) {
        super("Active quota not found for subscription: " + subscriptionId, HttpStatus.NOT_FOUND, "QUOTA_NOT_FOUND");
    }
}
