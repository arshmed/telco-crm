package com.telcocrm.identityservice.entity;

import com.telcocrm.identityservice.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var createdAt = LocalDateTime.now();
        var updatedAt = LocalDateTime.now();

        var user = User.builder()
                .id(id)
                .customerId(customerId)
                .username("agokhan")
                .email("agokhan@example.com")
                .fullName("Ahmet Gokhan")
                .phoneNumber("+905551112233")
                .status(UserStatus.ACTIVE)
                .keycloakUserId("kc-123")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(1L)
                .build();

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getCustomerId()).isEqualTo(customerId);
        assertThat(user.getUsername()).isEqualTo("agokhan");
        assertThat(user.getEmail()).isEqualTo("agokhan@example.com");
        assertThat(user.getFullName()).isEqualTo("Ahmet Gokhan");
        assertThat(user.getPhoneNumber()).isEqualTo("+905551112233");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getKeycloakUserId()).isEqualTo("kc-123");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(user.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldAllowNullCustomerIdAndKeycloakUserId() {
        var user = User.builder()
                .username("staffuser")
                .email("staff@example.com")
                .fullName("Staff User")
                .status(UserStatus.ACTIVE)
                .build();

        assertThat(user.getCustomerId()).isNull();
        assertThat(user.getKeycloakUserId()).isNull();
    }
}
