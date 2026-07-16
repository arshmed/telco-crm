package com.telcocrm.billingservice.exception;

public class OutboxPersistenceException extends RuntimeException {

    public OutboxPersistenceException(String topic, String aggregateId, Throwable cause) {
        super(String.format("Failed to persist outbox event: %s for aggregateId: %s", topic, aggregateId), cause);
    }
}
