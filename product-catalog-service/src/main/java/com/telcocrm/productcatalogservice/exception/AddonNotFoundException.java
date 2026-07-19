package com.telcocrm.productcatalogservice.exception;

import org.springframework.http.HttpStatus;

public class AddonNotFoundException extends BaseException {

    public AddonNotFoundException(String code) {
        super(
                "Addon not found with code: " + code,
                HttpStatus.NOT_FOUND,
                "ADDON_NOT_FOUND"
        );
    }
}
