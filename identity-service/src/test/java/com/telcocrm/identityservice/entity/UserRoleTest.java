package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var user = User.builder().id(UUID.randomUUID()).build();
        var role = Role.builder().id(UUID.randomUUID()).name("CUSTOMER").build();
        var assignedAt = LocalDateTime.now();

        var userRole = UserRole.builder()
                .id(id)
                .user(user)
                .role(role)
                .assignedAt(assignedAt)
                .assignedBy("service-account-order-service")
                .build();

        assertThat(userRole.getId()).isEqualTo(id);
        assertThat(userRole.getUser()).isEqualTo(user);
        assertThat(userRole.getRole()).isEqualTo(role);
        assertThat(userRole.getAssignedAt()).isEqualTo(assignedAt);
        assertThat(userRole.getAssignedBy()).isEqualTo("service-account-order-service");
    }
}
