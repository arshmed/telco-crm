package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.entity.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private final RoleMapper roleMapper = new RoleMapperImpl();

    @Test
    void shouldMapAllFields() {
        var role = Role.builder()
                .id(UUID.randomUUID())
                .name("SYSTEM_ADMIN")
                .description("Sistem genelinde kullanici yonetimi")
                .build();

        var response = roleMapper.toResponse(role, List.of("USER_WRITE", "USER_READ"));

        assertThat(response.id()).isEqualTo(role.getId());
        assertThat(response.name()).isEqualTo("SYSTEM_ADMIN");
        assertThat(response.description()).isEqualTo("Sistem genelinde kullanici yonetimi");
        assertThat(response.permissions()).containsExactly("USER_WRITE", "USER_READ");
    }

    @Test
    void shouldReturnEmptyPermissionsWhenNoneAssigned() {
        var role = Role.builder().id(UUID.randomUUID()).name("CUSTOMER").build();

        var response = roleMapper.toResponse(role, List.of());

        assertThat(response.permissions()).isEmpty();
    }

    @Test
    void shouldReturnNullWhenRoleAndPermissionsNull() {
        assertThat(roleMapper.toResponse(null, null)).isNull();
    }
}
