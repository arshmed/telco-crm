package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(UUID userId) {
        super(
            "User not found with id: " + userId,
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND"
        );
    }
}
