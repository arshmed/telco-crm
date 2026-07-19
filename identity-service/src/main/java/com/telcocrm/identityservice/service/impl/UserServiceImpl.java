package com.telcocrm.identityservice.service.impl;

import com.telcocrm.identityservice.dto.request.CreateUserRequest;
import com.telcocrm.identityservice.dto.request.UpdateUserRequest;
import com.telcocrm.identityservice.dto.response.UserResponse;
import com.telcocrm.identityservice.entity.Role;
import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.UserRole;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import com.telcocrm.identityservice.event.publish.RoleAssignedEvent;
import com.telcocrm.identityservice.event.publish.UserCreatedEvent;
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
import com.telcocrm.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final KeycloakSyncService keycloakSyncService;
    private final IdentityAuditService identityAuditService;
    private final OutboxService outboxService;
    private final UserStateRules userStateRules;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .customerId(request.customerId())
                .username(request.username())
                .email(request.email())
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        try {
            String keycloakUserId = keycloakSyncService.syncUserCreation(user);
            user.setKeycloakUserId(keycloakUserId);
            userRepository.save(user);
        } catch (KeycloakSyncException e) {
            log.warn("Keycloak sync failed while creating user: {} - {}", user.getUsername(), e.getMessage());
        }

        identityAuditService.log("USER", user.getId(), "CREATED", "User created: " + user.getUsername());

        outboxService.saveEvent(
                "USER",
                user.getId().toString(),
                "user-created-topic",
                UserCreatedEvent.of(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getCustomerId()));

        return userMapper.toResponse(user, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toResponse(user, roleNamesOf(userId));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            userRepository.findByEmail(request.email())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new DuplicateEmailException(request.email());
                    });
            user.setEmail(request.email());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }

        userRepository.save(user);

        identityAuditService.log("USER", user.getId(), "UPDATED", "User updated: " + user.getUsername());

        return userMapper.toResponse(user, roleNamesOf(userId));
    }

    @Override
    @Transactional
    public UserResponse assignRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        userStateRules.requireActiveForRoleAssignment(user);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        boolean alreadyAssigned = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(userRole -> userRole.getRole().getId().equals(role.getId()));
        if (alreadyAssigned) {
            throw new RoleAlreadyAssignedException(userId, roleName);
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedAt(LocalDateTime.now())
                .assignedBy(identityAuditService.resolvePerformedBy())
                .build();
        userRoleRepository.save(userRole);

        if (user.getKeycloakUserId() != null) {
            try {
                keycloakSyncService.syncRoleAssignment(user.getKeycloakUserId(), roleName);
            } catch (KeycloakSyncException e) {
                log.warn("Keycloak role sync failed for user: {} role: {} - {}", user.getUsername(), roleName, e.getMessage());
            }
        } else {
            log.warn("User: {} has no Keycloak account yet, skipping realm role sync for role: {}", user.getUsername(), roleName);
        }

        identityAuditService.log("USER", user.getId(), "ROLE_ASSIGNED", "Role assigned: " + roleName);

        outboxService.saveEvent(
                "USER",
                user.getId().toString(),
                "role-assigned-topic",
                RoleAssignedEvent.of(user.getId(), user.getUsername(), roleName));

        return userMapper.toResponse(user, roleNamesOf(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> userMapper.toResponse(user, roleNamesOf(user.getId())));
    }

    private List<String> roleNamesOf(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }
}
