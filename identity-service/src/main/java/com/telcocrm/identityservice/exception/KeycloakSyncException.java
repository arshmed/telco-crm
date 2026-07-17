package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

public class KeycloakSyncException extends BaseException {

    public KeycloakSyncException(String message) {
        super(
            message,
            HttpStatus.SERVICE_UNAVAILABLE,
            "KEYCLOAK_SYNC_FAILED"
        );
    }
}
