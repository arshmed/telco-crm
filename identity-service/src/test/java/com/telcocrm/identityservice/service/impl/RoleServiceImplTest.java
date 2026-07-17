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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private IdentityAuditService identityAuditService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void createRole_shouldPersistAndReturnResponse() {
        var request = new CreateRoleRequest("FIELD_DEALER", "Sahada yeni musteri kaydi");
        var response = mock(RoleResponse.class);

        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.empty());
        when(roleMapper.toResponse(any(Role.class), eq(List.of()))).thenReturn(response);

        RoleResponse result = roleService.createRole(request);

        assertThat(result).isEqualTo(response);
        verify(roleRepository).save(any(Role.class));
        verify(identityAuditService).log(eq("ROLE"), any(), eq("CREATED"), any());
    }

    @Test
    void createRole_shouldThrowWhenRoleAlreadyExists() {
        var request = new CreateRoleRequest("FIELD_DEALER", "desc");
        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.of(Role.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIELD_DEALER");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void getRoleByName_shouldReturnMappedResponseWithPermissions() {
        var role = Role.builder().id(UUID.randomUUID()).name("SYSTEM_ADMIN").build();
        var permission = Permission.builder().id(UUID.randomUUID()).name("USER_WRITE").build();
        var rolePermission = RolePermission.builder().role(role).permission(permission).build();
        var response = mock(RoleResponse.class);

        when(roleRepository.findByName("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findByRoleId(role.getId())).thenReturn(List.of(rolePermission));
        when(roleMapper.toResponse(role, List.of("USER_WRITE"))).thenReturn(response);

        RoleResponse result = roleService.getRoleByName("SYSTEM_ADMIN");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getRoleByName_shouldThrowWhenNotFound() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleByName("UNKNOWN"))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void listRoles_shouldMapEachRoleWithItsPermissions() {
        var role = Role.builder().id(UUID.randomUUID()).name("CUSTOMER").build();
        var response = mock(RoleResponse.class);

        when(roleRepository.findAll()).thenReturn(List.of(role));
        when(rolePermissionRepository.findByRoleId(role.getId())).thenReturn(List.of());
        when(roleMapper.toResponse(role, List.of())).thenReturn(response);

        List<RoleResponse> result = roleService.listRoles();

        assertThat(result).containsExactly(response);
    }

    @Test
    void assignPermission_shouldSaveAndReturnUpdatedRole() {
        var role = Role.builder().id(UUID.randomUUID()).name("SYSTEM_ADMIN").build();
        var permission = Permission.builder().id(UUID.randomUUID()).name("USER_WRITE").build();
        var response = mock(RoleResponse.class);

        when(roleRepository.findByName("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("USER_WRITE")).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByRoleId(role.getId())).thenReturn(List.of());
        when(identityAuditService.resolvePerformedBy()).thenReturn("SYSTEM");
        when(roleMapper.toResponse(role, List.of())).thenReturn(response);

        RoleResponse result = roleService.assignPermission("SYSTEM_ADMIN", "USER_WRITE");

        assertThat(result).isEqualTo(response);
        verify(rolePermissionRepository).save(any(RolePermission.class));
        verify(identityAuditService).log(eq("ROLE"), eq(role.getId()), eq("PERMISSION_ASSIGNED"), any());
    }

    @Test
    void assignPermission_shouldThrowWhenRoleNotFound() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignPermission("UNKNOWN", "USER_WRITE"))
                .isInstanceOf(RoleNotFoundException.class);
        verify(rolePermissionRepository, never()).save(any());
    }

    @Test
    void assignPermission_shouldThrowWhenPermissionNotFound() {
        var role = Role.builder().id(UUID.randomUUID()).name("SYSTEM_ADMIN").build();
        when(roleRepository.findByName("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignPermission("SYSTEM_ADMIN", "UNKNOWN"))
                .isInstanceOf(PermissionNotFoundException.class);
        verify(rolePermissionRepository, never()).save(any());
    }

    @Test
    void assignPermission_shouldThrowWhenAlreadyAssigned() {
        var role = Role.builder().id(UUID.randomUUID()).name("SYSTEM_ADMIN").build();
        var permission = Permission.builder().id(UUID.randomUUID()).name("USER_WRITE").build();
        var existing = RolePermission.builder().role(role).permission(permission).build();

        when(roleRepository.findByName("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("USER_WRITE")).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.findByRoleId(role.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> roleService.assignPermission("SYSTEM_ADMIN", "USER_WRITE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USER_WRITE")
                .hasMessageContaining("SYSTEM_ADMIN");
        verify(rolePermissionRepository, never()).save(any());
    }
}
