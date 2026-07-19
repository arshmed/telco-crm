package com.telcocrm.subscriptionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTariffRequest {

    @NotBlank(message = "Tariff code is required")
    private String tariffCode;
}
