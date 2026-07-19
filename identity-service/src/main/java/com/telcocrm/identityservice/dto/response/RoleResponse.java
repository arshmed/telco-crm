package com.telcocrm.identityservice.dto.response;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        List<String> permissions
) {}
