package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();

        var permission = Permission.builder()
                .id(id)
                .name("USER_WRITE")
                .description("Kullanici yazma yetkisi")
                .build();

        assertThat(permission.getId()).isEqualTo(id);
        assertThat(permission.getName()).isEqualTo("USER_WRITE");
        assertThat(permission.getDescription()).isEqualTo("Kullanici yazma yetkisi");
    }
}
