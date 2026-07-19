package com.telcocrm.identityservice.service.impl;

import com.telcocrm.identityservice.dto.request.CreateUserRequest;
import com.telcocrm.identityservice.dto.request.UpdateUserRequest;
import com.telcocrm.identityservice.dto.response.UserResponse;
import com.telcocrm.identityservice.entity.Role;
import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.UserRole;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import com.telcocrm.identityservice.exception.DuplicateEmailException;
import com.telcocrm.identityservice.exception.DuplicateUsernameException;
import com.telcocrm.identityservice.exception.KeycloakSyncException;
import com.telcocrm.identityservice.exception.RoleAlreadyAssignedException;
import com.telcocrm.identityservice.exception.RoleNotFoundException;
import com.telcocrm.identityservice.exception.UserNotFoundException;
import com.telcocrm.identityservice.mapper.UserMapper;
import com.telcocrm.identityservice.repository.RoleRepository;
import com.telcocrm.identityservice.repository.UserRepository;
import com.telcocrm.identityservice.repository.UserRoleRepository;
import com.telcocrm.identityservice.rules.UserStateRules;
import com.telcocrm.identityservice.service.IdentityAuditService;
import com.telcocrm.identityservice.service.KeycloakSyncService;
import com.telcocrm.identityservice.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KeycloakSyncService keycloakSyncService;

    @Mock
    private IdentityAuditService identityAuditService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UserStateRules userStateRules;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private CreateUserRequest validRequest() {
        return new CreateUserRequest("agokhan", "agokhan@example.com", "Ahmet Gokhan", "+905551112233", null);
    }

    @Test
    void createUser_shouldPersistAndSyncToKeycloak() {
        var request = validRequest();
        when(userRepository.existsByUsername("agokhan")).thenReturn(false);
        when(userRepository.findByEmail("agokhan@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            return u;
        });
        when(keycloakSyncService.syncUserCreation(any(User.class))).thenReturn("kc-user-1");
        when(userMapper.toResponse(any(User.class), eq(List.of()))).thenReturn(mock(UserResponse.class));

        UserResponse result = userService.createUser(request);

        assertThat(result).isNotNull();
        verify(userRepository, times(2)).save(userCaptor.capture());
        User finalSave = userCaptor.getValue();
        assertThat(finalSave.getKeycloakUserId()).isEqualTo("kc-user-1");
        assertThat(finalSave.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(identityAuditService).log(eq("USER"), any(), eq("CREATED"), any());
        verify(outboxService).saveEvent(eq("USER"), any(), eq("user-created-topic"), any());
    }

    @Test
    void createUser_shouldPersistUserEvenWhenKeycloakSyncFails() {
        var request = validRequest();
        when(userRepository.existsByUsername("agokhan")).thenReturn(false);
        when(userRepository.findByEmail("agokhan@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            return u;
        });
        when(keycloakSyncService.syncUserCreation(any(User.class)))
                .thenThrow(new KeycloakSyncException("invalid_client"));
        when(userMapper.toResponse(any(User.class), eq(List.of()))).thenReturn(mock(UserResponse.class));

        UserResponse result = userService.createUser(request);

        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
        verify(identityAuditService).log(eq("USER"), any(), eq("CREATED"), any());
        verify(outboxService).saveEvent(eq("USER"), any(), eq("user-created-topic"), any());
    }

    @Test
    void createUser_shouldThrowWhenUsernameTaken() {
        var request = validRequest();
        when(userRepository.existsByUsername("agokhan")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void createUser_shouldThrowWhenEmailTaken() {
        var request = validRequest();
        when(userRepository.existsByUsername("agokhan")).thenReturn(false);
        when(userRepository.findByEmail("agokhan@example.com")).thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_shouldReturnMappedResponseWithRoles() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        var role = Role.builder().id(UUID.randomUUID()).name("FIELD_DEALER").build();
        var userRole = UserRole.builder().user(user).role(role).build();
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(userRole));
        when(userMapper.toResponse(user, List.of("FIELD_DEALER"))).thenReturn(response);

        UserResponse result = userService.getUserById(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        var userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void updateUser_shouldApplyPartialFields() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).email("old@example.com").fullName("Old Name").status(UserStatus.ACTIVE).build();
        var request = new UpdateUserRequest("new@example.com", "New Name", null);
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        UserResponse result = userService.updateUser(userId, request);

        assertThat(result).isEqualTo(response);
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getFullName()).isEqualTo("New Name");
        verify(identityAuditService).log(eq("USER"), eq(userId), eq("UPDATED"), any());
    }

    @Test
    void updateUser_shouldThrowWhenNewEmailBelongsToAnotherUser() {
        var userId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();
        var user = User.builder().id(userId).email("old@example.com").status(UserStatus.ACTIVE).build();
        var otherUser = User.builder().id(otherUserId).email("taken@example.com").build();
        var request = new UpdateUserRequest("taken@example.com", null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateUser_shouldAllowKeepingSameEmail() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).email("same@example.com").status(UserStatus.ACTIVE).build();
        var request = new UpdateUserRequest("same@example.com", null, null);
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        userService.updateUser(userId, request);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void assignRole_shouldSaveAndSyncToKeycloakWhenUserAlreadySynced() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).username("agokhan").status(UserStatus.ACTIVE).keycloakUserId("kc-user-1").build();
        var role = Role.builder().id(UUID.randomUUID()).name("FIELD_DEALER").build();
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(identityAuditService.resolvePerformedBy()).thenReturn("SYSTEM");
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        UserResponse result = userService.assignRole(userId, "FIELD_DEALER");

        assertThat(result).isEqualTo(response);
        verify(userRoleRepository).save(any(UserRole.class));
        verify(keycloakSyncService).syncRoleAssignment("kc-user-1", "FIELD_DEALER");
        verify(identityAuditService).log(eq("USER"), eq(userId), eq("ROLE_ASSIGNED"), any());
        verify(outboxService).saveEvent(eq("USER"), any(), eq("role-assigned-topic"), any());
    }

    @Test
    void assignRole_shouldSkipKeycloakSyncWhenUserNotYetSynced() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).username("agokhan").status(UserStatus.ACTIVE).keycloakUserId(null).build();
        var role = Role.builder().id(UUID.randomUUID()).name("FIELD_DEALER").build();
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(identityAuditService.resolvePerformedBy()).thenReturn("SYSTEM");
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        userService.assignRole(userId, "FIELD_DEALER");

        verify(keycloakSyncService, never()).syncRoleAssignment(any(), any());
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void assignRole_shouldContinueWhenKeycloakSyncFails() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).username("agokhan").status(UserStatus.ACTIVE).keycloakUserId("kc-user-1").build();
        var role = Role.builder().id(UUID.randomUUID()).name("FIELD_DEALER").build();
        var response = mock(UserResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(identityAuditService.resolvePerformedBy()).thenReturn("SYSTEM");
        doThrow(new KeycloakSyncException("403 Forbidden")).when(keycloakSyncService).syncRoleAssignment("kc-user-1", "FIELD_DEALER");
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        UserResponse result = userService.assignRole(userId, "FIELD_DEALER");

        assertThat(result).isEqualTo(response);
        verify(userRoleRepository).save(any(UserRole.class));
        verify(outboxService).saveEvent(eq("USER"), any(), eq("role-assigned-topic"), any());
    }

    @Test
    void assignRole_shouldThrowWhenUserNotActive() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).status(UserStatus.INACTIVE).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Only ACTIVE users can be assigned roles"))
                .when(userStateRules).requireActiveForRoleAssignment(user);

        assertThatThrownBy(() -> userService.assignRole(userId, "FIELD_DEALER"))
                .isInstanceOf(IllegalStateException.class);
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void assignRole_shouldThrowWhenRoleNotFound() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).status(UserStatus.ACTIVE).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignRole(userId, "UNKNOWN"))
                .isInstanceOf(RoleNotFoundException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignRole_shouldThrowWhenAlreadyAssigned() {
        var userId = UUID.randomUUID();
        var user = User.builder().id(userId).status(UserStatus.ACTIVE).build();
        var role = Role.builder().id(UUID.randomUUID()).name("FIELD_DEALER").build();
        var existingUserRole = UserRole.builder().user(user).role(role).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("FIELD_DEALER")).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(existingUserRole));

        assertThatThrownBy(() -> userService.assignRole(userId, "FIELD_DEALER"))
                .isInstanceOf(RoleAlreadyAssignedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void listUsers_shouldMapEachUserWithItsRoles() {
        var user = User.builder().id(UUID.randomUUID()).status(UserStatus.ACTIVE).build();
        var response = mock(UserResponse.class);
        var page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(userMapper.toResponse(user, List.of())).thenReturn(response);

        Page<UserResponse> result = userService.listUsers(Pageable.unpaged());

        assertThat(result.getContent()).containsExactly(response);
    }
}
