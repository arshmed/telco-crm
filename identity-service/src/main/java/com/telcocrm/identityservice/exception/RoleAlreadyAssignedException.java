package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RoleAlreadyAssignedException extends BaseException {

    public RoleAlreadyAssignedException(UUID userId, String roleName) {
        super(
            "Role: " + roleName + " is already assigned to user with id: " + userId,
            HttpStatus.CONFLICT,
            "ROLE_ALREADY_ASSIGNED"
        );
    }
}
