package com.telcocrm.usageservice.exception;

import org.springframework.http.HttpStatus;

public class TariffNotFoundException extends BaseException {

    public TariffNotFoundException(String code) {
        super("Tariff not found with code: " + code, HttpStatus.NOT_FOUND, "TARIFF_NOT_FOUND");
    }
}
