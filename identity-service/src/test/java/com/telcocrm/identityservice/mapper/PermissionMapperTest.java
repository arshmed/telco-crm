package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.entity.Permission;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionMapperTest {

    private final PermissionMapper permissionMapper = new PermissionMapperImpl();

    @Test
    void shouldMapAllFields() {
        var permission = Permission.builder()
                .id(UUID.randomUUID())
                .name("USER_WRITE")
                .description("Kullanici yazma yetkisi")
                .build();

        var response = permissionMapper.toResponse(permission);

        assertThat(response.id()).isEqualTo(permission.getId());
        assertThat(response.name()).isEqualTo("USER_WRITE");
        assertThat(response.description()).isEqualTo("Kullanici yazma yetkisi");
    }

    @Test
    void shouldReturnNullWhenPermissionNull() {
        assertThat(permissionMapper.toResponse(null)).isNull();
    }
}
