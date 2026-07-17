package com.telcocrm.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.identityservice.dto.request.AssignRoleRequest;
import com.telcocrm.identityservice.dto.request.CreateUserRequest;
import com.telcocrm.identityservice.dto.request.UpdateUserRequest;
import com.telcocrm.identityservice.dto.response.UserResponse;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import com.telcocrm.identityservice.exception.DuplicateUsernameException;
import com.telcocrm.identityservice.exception.GlobalExceptionHandler;
import com.telcocrm.identityservice.exception.RoleAlreadyAssignedException;
import com.telcocrm.identityservice.exception.RoleNotFoundException;
import com.telcocrm.identityservice.exception.UserNotFoundException;
import com.telcocrm.identityservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(
                new SpringDataJacksonConfiguration.PageModule(
                        new SpringDataWebSettings(
                                EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));
    }

    @BeforeEach
    void setUp() {
        var controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private UserResponse sampleResponse() {
        return new UserResponse(
                UUID.randomUUID(),
                null,
                "agokhan",
                "agokhan@example.com",
                "Ahmet Gokhan",
                "+905551112233",
                UserStatus.ACTIVE,
                "kc-user-1",
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createUser_shouldReturn201() throws Exception {
        var request = new CreateUserRequest("agokhan", "agokhan@example.com", "Ahmet Gokhan", "+905551112233", null);
        var response = sampleResponse();
        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/" + response.id()))
                .andExpect(jsonPath("$.username").value("agokhan"));
    }

    @Test
    void createUser_shouldReturn400WhenUsernameBlank() throws Exception {
        var request = new CreateUserRequest("", "agokhan@example.com", "Ahmet Gokhan", null, null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturn400WhenEmailInvalid() throws Exception {
        var request = new CreateUserRequest("agokhan", "not-an-email", "Ahmet Gokhan", null, null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturn409WhenUsernameTaken() throws Exception {
        var request = new CreateUserRequest("agokhan", "agokhan@example.com", "Ahmet Gokhan", null, null);
        when(userService.createUser(any())).thenThrow(new DuplicateUsernameException("agokhan"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getUserById_shouldReturn200() throws Exception {
        var response = sampleResponse();
        when(userService.getUserById(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void getUserById_shouldReturn404WhenNotFound() throws Exception {
        var userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listUsers_shouldReturn200() throws Exception {
        when(userService.listUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("agokhan"));
    }

    @Test
    void updateUser_shouldReturn200() throws Exception {
        var request = new UpdateUserRequest("new@example.com", "New Name", null);
        var response = sampleResponse();
        when(userService.updateUser(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/{id}", response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("agokhan"));
    }

    @Test
    void updateUser_shouldReturn400WhenEmailInvalid() throws Exception {
        var request = new UpdateUserRequest("not-an-email", null, null);

        mockMvc.perform(put("/api/v1/users/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignRole_shouldReturn200() throws Exception {
        var request = new AssignRoleRequest("FIELD_DEALER");
        var response = sampleResponse();
        when(userService.assignRole(any(), eq("FIELD_DEALER"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/{id}/roles", response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void assignRole_shouldReturn400WhenRoleNameBlank() throws Exception {
        var request = new AssignRoleRequest("");

        mockMvc.perform(post("/api/v1/users/{id}/roles", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignRole_shouldReturn404WhenRoleNotFound() throws Exception {
        var request = new AssignRoleRequest("UNKNOWN");
        var userId = UUID.randomUUID();
        when(userService.assignRole(any(), eq("UNKNOWN"))).thenThrow(new RoleNotFoundException("UNKNOWN"));

        mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignRole_shouldReturn409WhenAlreadyAssigned() throws Exception {
        var userId = UUID.randomUUID();
        var request = new AssignRoleRequest("FIELD_DEALER");
        when(userService.assignRole(any(), eq("FIELD_DEALER"))).thenThrow(new RoleAlreadyAssignedException(userId, "FIELD_DEALER"));

        mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
