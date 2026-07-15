package com.telcocrm.subscriptionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private UUID id;
    private String customerNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String identityNumber;
}
