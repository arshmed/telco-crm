package com.telcocrm.productcatalogservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TariffPriceChangeRequest(

        @NotNull
        @PositiveOrZero
        BigDecimal monthlyFee
) {
}
