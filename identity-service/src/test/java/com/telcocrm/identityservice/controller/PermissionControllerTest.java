package com.telcocrm.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.identityservice.dto.request.CreatePermissionRequest;
import com.telcocrm.identityservice.dto.response.PermissionResponse;
import com.telcocrm.identityservice.exception.GlobalExceptionHandler;
import com.telcocrm.identityservice.service.PermissionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PermissionService permissionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new PermissionController(permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PermissionResponse sampleResponse() {
        return new PermissionResponse(UUID.randomUUID(), "USER_WRITE", "Kullanici yazma yetkisi");
    }

    @Test
    void createPermission_shouldReturn201() throws Exception {
        var request = new CreatePermissionRequest("USER_WRITE", "Kullanici yazma yetkisi");
        var response = sampleResponse();
        when(permissionService.createPermission(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/permissions/" + response.id()))
                .andExpect(jsonPath("$.name").value("USER_WRITE"));
    }

    @Test
    void createPermission_shouldReturn400WhenNameBlank() throws Exception {
        var request = new CreatePermissionRequest("", "desc");

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPermission_shouldReturn409WhenAlreadyExists() throws Exception {
        var request = new CreatePermissionRequest("USER_WRITE", "desc");
        when(permissionService.createPermission(any())).thenThrow(new IllegalStateException("Permission already exists with name: USER_WRITE"));

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listPermissions_shouldReturn200() throws Exception {
        when(permissionService.listPermissions()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("USER_WRITE"));
    }
}
