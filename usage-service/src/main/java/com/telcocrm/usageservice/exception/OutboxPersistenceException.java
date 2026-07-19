package com.telcocrm.usageservice.exception;

import org.springframework.http.HttpStatus;

public class OutboxPersistenceException extends BaseException {

    public OutboxPersistenceException(String topic, String aggregateId, Throwable cause) {
        super("Failed to persist outbox event for topic: " + topic + ", aggregateId: " + aggregateId,
                HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_PERSISTENCE_ERROR");
        initCause(cause);
    }
}
