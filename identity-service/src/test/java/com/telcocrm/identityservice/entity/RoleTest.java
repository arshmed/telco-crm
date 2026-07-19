package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();

        var role = Role.builder()
                .id(id)
                .name("FIELD_DEALER")
                .description("Sahada yeni musteri kaydi")
                .build();

        assertThat(role.getId()).isEqualTo(id);
        assertThat(role.getName()).isEqualTo("FIELD_DEALER");
        assertThat(role.getDescription()).isEqualTo("Sahada yeni musteri kaydi");
    }
}
