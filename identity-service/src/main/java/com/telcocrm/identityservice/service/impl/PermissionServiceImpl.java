package com.telcocrm.identityservice.service.impl;

import com.telcocrm.identityservice.dto.request.CreatePermissionRequest;
import com.telcocrm.identityservice.dto.response.PermissionResponse;
import com.telcocrm.identityservice.entity.Permission;
import com.telcocrm.identityservice.mapper.PermissionMapper;
import com.telcocrm.identityservice.repository.PermissionRepository;
import com.telcocrm.identityservice.service.IdentityAuditService;
import com.telcocrm.identityservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final IdentityAuditService identityAuditService;

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.findByName(request.name()).isPresent()) {
            throw new IllegalStateException("Permission already exists with name: " + request.name());
        }

        Permission permission = Permission.builder()
                .name(request.name())
                .description(request.description())
                .build();
        permissionRepository.save(permission);

        identityAuditService.log("PERMISSION", permission.getId(), "CREATED", "Permission created: " + permission.getName());

        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}
