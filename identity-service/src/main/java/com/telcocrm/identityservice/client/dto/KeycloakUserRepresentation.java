package com.telcocrm.identityservice.client.dto;

public record KeycloakUserRepresentation(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled
) {}
