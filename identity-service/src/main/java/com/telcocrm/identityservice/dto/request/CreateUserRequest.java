package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateUserRequest(

        @NotBlank(message = "Username must not be blank")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Full name must not be blank")
        String fullName,

        String phoneNumber,

        UUID customerId
) {}
