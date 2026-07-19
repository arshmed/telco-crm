package com.telcocrm.billingservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillCycleResponse {

    private UUID id;
    private UUID customerId;
    private String customerNo;
    private UUID subscriptionId;
    private String tariffCode;
    private Integer dayOfMonth;
    private LocalDate nextRunDate;
    private Boolean active;
}
