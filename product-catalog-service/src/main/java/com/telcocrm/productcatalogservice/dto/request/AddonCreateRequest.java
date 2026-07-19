package com.telcocrm.productcatalogservice.dto.request;

import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AddonCreateRequest(

        @NotBlank
        String code,

        @NotBlank
        String name,

        @NotNull
        AddonType type,

        @NotNull
        @PositiveOrZero
        BigDecimal price,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @PositiveOrZero
        Integer validityDays
) {
}
