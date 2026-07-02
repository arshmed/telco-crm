package com.telcocrm.productcatalogservice.dto.request;

import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;

public record TariffUpdateRequest(

        @NotBlank
        String name,

        @NotNull
        TariffSegment segment,

        @NotNull
        @PositiveOrZero
        BigDecimal monthlyFee,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @PositiveOrZero
        Integer minutesIncluded,

        @PositiveOrZero
        Integer smsIncluded,

        @PositiveOrZero
        Integer dataMbIncluded,

        Set<String> addonCodes
) {
}
