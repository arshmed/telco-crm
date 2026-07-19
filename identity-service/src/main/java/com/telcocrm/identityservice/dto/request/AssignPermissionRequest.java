package com.telcocrm.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignPermissionRequest(

        @NotBlank(message = "Permission name must not be blank")
        String permissionName
) {}
