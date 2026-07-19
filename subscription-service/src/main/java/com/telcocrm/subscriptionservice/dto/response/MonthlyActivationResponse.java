package com.telcocrm.subscriptionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyActivationResponse {
    private String month;
    private long count;
}
