package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void shouldMapAllFields() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .username("agokhan")
                .email("agokhan@example.com")
                .fullName("Ahmet Gokhan")
                .phoneNumber("+905551112233")
                .status(UserStatus.ACTIVE)
                .keycloakUserId("kc-123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var response = userMapper.toResponse(user, List.of("FIELD_DEALER"));

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.customerId()).isEqualTo(user.getCustomerId());
        assertThat(response.username()).isEqualTo("agokhan");
        assertThat(response.email()).isEqualTo("agokhan@example.com");
        assertThat(response.fullName()).isEqualTo("Ahmet Gokhan");
        assertThat(response.phoneNumber()).isEqualTo("+905551112233");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.keycloakUserId()).isEqualTo("kc-123");
        assertThat(response.roles()).containsExactly("FIELD_DEALER");
    }

    @Test
    void shouldReturnEmptyRolesWhenNoneAssigned() {
        var user = User.builder().id(UUID.randomUUID()).status(UserStatus.ACTIVE).build();

        var response = userMapper.toResponse(user, List.of());

        assertThat(response.roles()).isEmpty();
    }

    @Test
    void shouldReturnNullWhenUserAndRolesNull() {
        assertThat(userMapper.toResponse(null, null)).isNull();
    }
}
