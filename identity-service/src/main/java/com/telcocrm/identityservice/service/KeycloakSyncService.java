package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.client.KeycloakAdminClient;
import com.telcocrm.identityservice.client.dto.KeycloakRoleRepresentation;
import com.telcocrm.identityservice.client.dto.KeycloakUserRepresentation;
import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.exception.KeycloakSyncException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakSyncService {

    private final KeycloakAdminClient keycloakAdminClient;

    @CircuitBreaker(name = "keycloakAdminCircuitBreaker", fallbackMethod = "syncUserCreationFallback")
    public String syncUserCreation(User user) {
        String[] nameParts = splitName(user.getFullName());

        KeycloakUserRepresentation representation = new KeycloakUserRepresentation(
                user.getUsername(),
                user.getEmail(),
                nameParts[0],
                nameParts[1],
                true,
                buildAttributes(user)
        );

        ResponseEntity<Void> response = keycloakAdminClient.createUser(representation);
        return extractUserId(response, user.getUsername());
    }

    String syncUserCreationFallback(User user, Throwable throwable) {
        throw new KeycloakSyncException(
                "Failed to sync user to Keycloak: " + user.getUsername() + " - " + throwable.getMessage());
    }

    @CircuitBreaker(name = "keycloakAdminCircuitBreaker", fallbackMethod = "syncRoleAssignmentFallback")
    public void syncRoleAssignment(String keycloakUserId, String roleName) {
        KeycloakRoleRepresentation role = keycloakAdminClient.getRealmRole(roleName);
        keycloakAdminClient.assignRealmRole(keycloakUserId, List.of(role));
    }

    void syncRoleAssignmentFallback(String keycloakUserId, String roleName, Throwable throwable) {
        throw new KeycloakSyncException(
                "Failed to assign realm role: " + roleName + " to Keycloak user: " + keycloakUserId + " - " + throwable.getMessage());
    }

    private String extractUserId(ResponseEntity<Void> response, String username) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new KeycloakSyncException("Keycloak did not return a Location header for created user: " + username);
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private Map<String, List<String>> buildAttributes(User user) {
        if (user.getCustomerId() == null) {
            return Map.of();
        }
        return Map.of("customer_id", List.of(user.getCustomerId().toString()));
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[] { trimmed, "" };
        }
        return new String[] { trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1).trim() };
    }
}
