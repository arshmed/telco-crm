package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var role = Role.builder().id(UUID.randomUUID()).name("SYSTEM_ADMIN").build();
        var permission = Permission.builder().id(UUID.randomUUID()).name("USER_WRITE").build();
        var assignedAt = LocalDateTime.now();

        var rolePermission = RolePermission.builder()
                .id(id)
                .role(role)
                .permission(permission)
                .assignedAt(assignedAt)
                .assignedBy("SYSTEM")
                .build();

        assertThat(rolePermission.getId()).isEqualTo(id);
        assertThat(rolePermission.getRole()).isEqualTo(role);
        assertThat(rolePermission.getPermission()).isEqualTo(permission);
        assertThat(rolePermission.getAssignedAt()).isEqualTo(assignedAt);
        assertThat(rolePermission.getAssignedBy()).isEqualTo("SYSTEM");
    }
}
