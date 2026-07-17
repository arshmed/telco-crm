package com.telcocrm.identityservice.dto.response;

import com.telcocrm.identityservice.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID customerId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        UserStatus status,
        String keycloakUserId,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
