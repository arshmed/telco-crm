package com.telcocrm.productcatalogservice.exception;

public class OutboxPersistenceException extends RuntimeException {

    public OutboxPersistenceException(String eventType, String aggregateId, Throwable cause) {
        super("Failed to persist outbox event [" + eventType + "] for aggregateId: " + aggregateId, cause);
    }
}
