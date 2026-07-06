package com.telcocrm.productcatalogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AddonUpdateRequest(

        @NotBlank
        String name,

        @NotNull
        @PositiveOrZero
        BigDecimal price,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @PositiveOrZero
        Integer validityDays
) {
}
