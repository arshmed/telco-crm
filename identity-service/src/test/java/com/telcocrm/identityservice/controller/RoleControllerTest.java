package com.telcocrm.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.identityservice.dto.request.AssignPermissionRequest;
import com.telcocrm.identityservice.dto.request.CreateRoleRequest;
import com.telcocrm.identityservice.dto.response.RoleResponse;
import com.telcocrm.identityservice.exception.GlobalExceptionHandler;
import com.telcocrm.identityservice.exception.PermissionNotFoundException;
import com.telcocrm.identityservice.exception.RoleNotFoundException;
import com.telcocrm.identityservice.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new RoleController(roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RoleResponse sampleResponse() {
        return new RoleResponse(UUID.randomUUID(), "FIELD_DEALER", "Sahada yeni musteri kaydi", List.of());
    }

    @Test
    void createRole_shouldReturn201() throws Exception {
        var request = new CreateRoleRequest("FIELD_DEALER", "Sahada yeni musteri kaydi");
        var response = sampleResponse();
        when(roleService.createRole(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/roles/" + response.name()))
                .andExpect(jsonPath("$.name").value("FIELD_DEALER"));
    }

    @Test
    void createRole_shouldReturn400WhenNameBlank() throws Exception {
        var request = new CreateRoleRequest("", "desc");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_shouldReturn409WhenAlreadyExists() throws Exception {
        var request = new CreateRoleRequest("FIELD_DEALER", "desc");
        when(roleService.createRole(any())).thenThrow(new IllegalStateException("Role already exists with name: FIELD_DEALER"));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listRoles_shouldReturn200() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FIELD_DEALER"));
    }

    @Test
    void assignPermission_shouldReturn200() throws Exception {
        var request = new AssignPermissionRequest("USER_WRITE");
        var response = sampleResponse();
        when(roleService.assignPermission(eq("FIELD_DEALER"), eq("USER_WRITE"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/roles/{name}/permissions", "FIELD_DEALER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void assignPermission_shouldReturn400WhenPermissionNameBlank() throws Exception {
        var request = new AssignPermissionRequest("");

        mockMvc.perform(post("/api/v1/roles/{name}/permissions", "FIELD_DEALER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignPermission_shouldReturn404WhenRoleNotFound() throws Exception {
        var request = new AssignPermissionRequest("USER_WRITE");
        when(roleService.assignPermission(eq("UNKNOWN"), eq("USER_WRITE"))).thenThrow(new RoleNotFoundException("UNKNOWN"));

        mockMvc.perform(post("/api/v1/roles/{name}/permissions", "UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignPermission_shouldReturn404WhenPermissionNotFound() throws Exception {
        var request = new AssignPermissionRequest("UNKNOWN");
        when(roleService.assignPermission(eq("FIELD_DEALER"), eq("UNKNOWN"))).thenThrow(new PermissionNotFoundException("UNKNOWN"));

        mockMvc.perform(post("/api/v1/roles/{name}/permissions", "FIELD_DEALER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
