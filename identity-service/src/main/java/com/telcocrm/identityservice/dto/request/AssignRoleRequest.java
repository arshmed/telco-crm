package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(

        @NotBlank(message = "Role name must not be blank")
        String roleName
) {}
