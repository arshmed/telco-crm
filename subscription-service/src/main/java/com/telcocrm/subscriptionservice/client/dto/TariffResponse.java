package com.telcocrm.subscriptionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffResponse {

    private String code;
    private String name;
    private BigDecimal price;
    private String currency;
}
