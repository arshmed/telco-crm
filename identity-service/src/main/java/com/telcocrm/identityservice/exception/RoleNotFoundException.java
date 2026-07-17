package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends BaseException {

    public RoleNotFoundException(String roleName) {
        super(
            "Role not found with name: " + roleName,
            HttpStatus.NOT_FOUND,
            "ROLE_NOT_FOUND"
        );
    }
}
