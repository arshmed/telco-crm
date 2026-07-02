package com.telcocrm.productcatalogservice.exception;

import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import org.springframework.http.HttpStatus;

public class InvalidTariffStatusException extends BaseException {

    public InvalidTariffStatusException(String code, TariffStatus status) {
        super(
                "Tariff " + code + " cannot be published from status: " + status,
                HttpStatus.CONFLICT,
                "INVALID_TARIFF_STATUS"
        );
    }
}
