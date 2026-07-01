package com.telcocrm.productcatalogservice.dto.request;

import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record TariffCreateRequest(

        @NotBlank
        String code,

        @NotBlank
        String name,

        @NotNull
        TariffType type,

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

        @NotNull
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        Set<String> addonCodes
) {
}
