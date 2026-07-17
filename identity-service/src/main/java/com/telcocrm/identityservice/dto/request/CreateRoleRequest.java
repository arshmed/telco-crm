package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(

        @NotBlank(message = "Role name must not be blank")
        String name,

        String description
) {}
