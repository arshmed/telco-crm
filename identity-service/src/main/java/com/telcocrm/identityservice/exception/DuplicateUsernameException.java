package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateUsernameException extends BaseException {

    public DuplicateUsernameException(String username) {
        super(
            "Username already in use: " + username,
            HttpStatus.CONFLICT,
            "DUPLICATE_USERNAME"
        );
    }
}
