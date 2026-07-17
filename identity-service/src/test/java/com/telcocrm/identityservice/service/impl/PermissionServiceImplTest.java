package com.telcocrm.identityservice.service.impl;

import com.telcocrm.identityservice.dto.request.CreatePermissionRequest;
import com.telcocrm.identityservice.dto.response.PermissionResponse;
import com.telcocrm.identityservice.entity.Permission;
import com.telcocrm.identityservice.mapper.PermissionMapper;
import com.telcocrm.identityservice.repository.PermissionRepository;
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
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private IdentityAuditService identityAuditService;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void createPermission_shouldPersistAndReturnResponse() {
        var request = new CreatePermissionRequest("USER_WRITE", "Kullanici yazma yetkisi");
        var response = mock(PermissionResponse.class);

        when(permissionRepository.findByName("USER_WRITE")).thenReturn(Optional.empty());
        when(permissionMapper.toResponse(any(Permission.class))).thenReturn(response);

        PermissionResponse result = permissionService.createPermission(request);

        assertThat(result).isEqualTo(response);
        verify(permissionRepository).save(any(Permission.class));
        verify(identityAuditService).log(eq("PERMISSION"), any(), eq("CREATED"), any());
    }

    @Test
    void createPermission_shouldThrowWhenAlreadyExists() {
        var request = new CreatePermissionRequest("USER_WRITE", "desc");
        when(permissionRepository.findByName("USER_WRITE")).thenReturn(Optional.of(Permission.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USER_WRITE");
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void listPermissions_shouldReturnMappedList() {
        var permission = Permission.builder().id(UUID.randomUUID()).name("USER_WRITE").build();
        var response = mock(PermissionResponse.class);

        when(permissionRepository.findAll()).thenReturn(List.of(permission));
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        List<PermissionResponse> result = permissionService.listPermissions();

        assertThat(result).containsExactly(response);
    }
}
