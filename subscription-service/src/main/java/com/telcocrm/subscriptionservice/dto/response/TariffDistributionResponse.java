package com.telcocrm.subscriptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TariffDistributionResponse {
    private String tariffCode;
    private long count;
}
