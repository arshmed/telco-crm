package com.telcocrm.identityservice.controller;

import com.telcocrm.identityservice.dto.request.AssignPermissionRequest;
import com.telcocrm.identityservice.dto.request.CreateRoleRequest;
import com.telcocrm.identityservice.dto.response.RoleResponse;
import com.telcocrm.identityservice.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.created(URI.create("/api/v1/roles/" + response.name())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @PostMapping("/{name}/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<RoleResponse> assignPermission(
            @PathVariable String name,
            @Valid @RequestBody AssignPermissionRequest request) {
        return ResponseEntity.ok(roleService.assignPermission(name, request.permissionName()));
    }
}
