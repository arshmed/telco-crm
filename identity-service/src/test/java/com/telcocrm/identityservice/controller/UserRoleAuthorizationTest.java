package com.telcocrm.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.identityservice.dto.request.AssignRoleRequest;
import com.telcocrm.identityservice.dto.response.UserResponse;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import com.telcocrm.identityservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void assignRole_withoutSystemAdminAuthority_shouldReturn403() throws Exception {
        var request = new AssignRoleRequest("FIELD_DEALER");

        mockMvc.perform(post("/api/v1/users/{id}/roles", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("CALL_CENTER_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignRole_withSystemAdminAuthority_shouldReturn200() throws Exception {
        var userId = UUID.randomUUID();
        var request = new AssignRoleRequest("FIELD_DEALER");
        var response = new UserResponse(
                userId, null, "agokhan", "agokhan@example.com", "Ahmet Gokhan",
                "+905551112233", UserStatus.ACTIVE, "kc-user-1", List.of(),
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.assignRole(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SYSTEM_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
