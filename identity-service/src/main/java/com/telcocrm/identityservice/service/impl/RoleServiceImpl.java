package com.telcocrm.identityservice.service.impl;

import com.telcocrm.identityservice.dto.request.CreateRoleRequest;
import com.telcocrm.identityservice.dto.response.RoleResponse;
import com.telcocrm.identityservice.entity.Permission;
import com.telcocrm.identityservice.entity.Role;
import com.telcocrm.identityservice.entity.RolePermission;
import com.telcocrm.identityservice.exception.PermissionNotFoundException;
import com.telcocrm.identityservice.exception.RoleNotFoundException;
import com.telcocrm.identityservice.mapper.RoleMapper;
import com.telcocrm.identityservice.repository.PermissionRepository;
import com.telcocrm.identityservice.repository.RolePermissionRepository;
import com.telcocrm.identityservice.repository.RoleRepository;
import com.telcocrm.identityservice.service.IdentityAuditService;
import com.telcocrm.identityservice.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMapper roleMapper;
    private final IdentityAuditService identityAuditService;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new IllegalStateException("Role already exists with name: " + request.name());
        }

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .build();
        roleRepository.save(role);

        identityAuditService.log("ROLE", role.getId(), "CREATED", "Role created: " + role.getName());

        return roleMapper.toResponse(role, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new RoleNotFoundException(name));

        return roleMapper.toResponse(role, permissionNamesOf(role.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(role -> roleMapper.toResponse(role, permissionNamesOf(role.getId())))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse assignPermission(String roleName, String permissionName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new PermissionNotFoundException(permissionName));

        boolean alreadyAssigned = rolePermissionRepository.findByRoleId(role.getId()).stream()
                .anyMatch(rolePermission -> rolePermission.getPermission().getId().equals(permission.getId()));
        if (alreadyAssigned) {
            throw new IllegalStateException("Permission: " + permissionName + " is already assigned to role: " + roleName);
        }

        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .assignedAt(LocalDateTime.now())
                .assignedBy(identityAuditService.resolvePerformedBy())
                .build();
        rolePermissionRepository.save(rolePermission);

        identityAuditService.log("ROLE", role.getId(), "PERMISSION_ASSIGNED", "Permission assigned: " + permissionName);

        return roleMapper.toResponse(role, permissionNamesOf(role.getId()));
    }

    private List<String> permissionNamesOf(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(rolePermission -> rolePermission.getPermission().getName())
                .toList();
    }
}
