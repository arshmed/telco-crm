package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(

        @NotBlank(message = "Permission name must not be blank")
        String name,

        String description
) {}
