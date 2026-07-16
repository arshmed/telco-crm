package com.telcocrm.subscriptionservice.exception;

public class MsisdnAlreadyInUseException extends RuntimeException {

    public MsisdnAlreadyInUseException(String message) {
        super(message);
    }
}
