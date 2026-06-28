package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

public class OutboxPersistenceException extends BaseException {

    public OutboxPersistenceException(String eventType, String aggregateId, Throwable cause) {
        super(
            "Failed to save outbox event: " + eventType + " for aggregateId: " + aggregateId,
            cause,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "OUTBOX_PERSISTENCE_FAILED"
        );
    }
}
