package com.telcocrm.identityservice.controller;

import com.telcocrm.identityservice.dto.request.CreatePermissionRequest;
import com.telcocrm.identityservice.dto.response.PermissionResponse;
import com.telcocrm.identityservice.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.created(URI.create("/api/v1/permissions/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }
}
