package com.telcocrm.customerservice.dto;

import com.telcocrm.customerservice.enums.CustomerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @NotNull
    private CustomerType type;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    private String identityNumber;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String phone;

    @Valid
    private List<AddressRequest> addresses;
}
