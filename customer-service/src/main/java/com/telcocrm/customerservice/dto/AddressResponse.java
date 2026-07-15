package com.telcocrm.customerservice.dto;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse implements Serializable {

    private UUID id;
    private String line1;
    private String city;
    private String district;
    private String postalCode;
    private Boolean isDefault;
}
