package com.telcocrm.identityservice.client.dto;

import java.util.List;
import java.util.Map;

public record KeycloakUserRepresentation(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Map<String, List<String>> attributes
) {}
