package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.dto.request.CreateUserRequest;
import com.telcocrm.identityservice.dto.request.UpdateUserRequest;
import com.telcocrm.identityservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID userId);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    UserResponse assignRole(UUID userId, String roleName);

    Page<UserResponse> listUsers(Pageable pageable);
}
