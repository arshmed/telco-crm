package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(

        @Email(message = "Email must be valid")
        String email,

        String fullName,

        String phoneNumber
) {}
