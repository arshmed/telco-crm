package com.telcocrm.productcatalogservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCodeException extends BaseException {

    public DuplicateCodeException(String code) {
        super(
                "Code already exists: " + code,
                HttpStatus.CONFLICT,
                "DUPLICATE_CODE"
        );
    }
}
