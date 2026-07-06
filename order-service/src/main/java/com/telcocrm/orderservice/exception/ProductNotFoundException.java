package com.telcocrm.orderservice.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BaseException {

    public ProductNotFoundException(String code) {
        super(
            "Product not found with code: " + code,
            HttpStatus.NOT_FOUND,
            "PRODUCT_NOT_FOUND"
        );
    }
}
