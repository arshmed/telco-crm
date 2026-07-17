package com.telcocrm.identityservice.exception;

import org.springframework.http.HttpStatus;

public class PermissionNotFoundException extends BaseException {

    public PermissionNotFoundException(String permissionName) {
        super(
            "Permission not found with name: " + permissionName,
            HttpStatus.NOT_FOUND,
            "PERMISSION_NOT_FOUND"
        );
    }
}
