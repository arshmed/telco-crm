package com.telcocrm.identityservice.client;

import com.telcocrm.identityservice.client.dto.KeycloakRoleRepresentation;
import com.telcocrm.identityservice.client.dto.KeycloakUserRepresentation;
import com.telcocrm.identityservice.config.KeycloakAdminClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "keycloak-admin-client",
        url = "${keycloak.admin.base-url}",
        configuration = KeycloakAdminClientConfig.class
)
public interface KeycloakAdminClient {

    @PostMapping("/users")
    ResponseEntity<Void> createUser(@RequestBody KeycloakUserRepresentation user);

    @GetMapping("/roles/{roleName}")
    KeycloakRoleRepresentation getRealmRole(@PathVariable String roleName);

    @PostMapping("/users/{userId}/role-mappings/realm")
    void assignRealmRole(@PathVariable String userId, @RequestBody List<KeycloakRoleRepresentation> roles);
}
