package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BaseException {

    public DuplicateEmailException(String email) {
        super(
            "Email already in use: " + email,
            HttpStatus.CONFLICT,
            "DUPLICATE_EMAIL"
        );
    }
}
