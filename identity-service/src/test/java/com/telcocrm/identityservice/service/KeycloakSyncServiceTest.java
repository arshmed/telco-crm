package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.client.KeycloakAdminClient;
import com.telcocrm.identityservice.client.dto.KeycloakRoleRepresentation;
import com.telcocrm.identityservice.client.dto.KeycloakUserRepresentation;
import com.telcocrm.identityservice.entity.User;
import com.telcocrm.identityservice.entity.enums.UserStatus;
import com.telcocrm.identityservice.exception.KeycloakSyncException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakSyncServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private KeycloakSyncService keycloakSyncService;

    @Captor
    private ArgumentCaptor<KeycloakUserRepresentation> userRepresentationCaptor;

    private User aUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("agokhan")
                .email("agokhan@example.com")
                .fullName("Ahmet Gokhan")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void syncUserCreation_shouldExtractIdFromLocationHeader() {
        var user = aUser();
        var keycloakId = UUID.randomUUID().toString();
        ResponseEntity<Void> response = ResponseEntity.created(URI.create("/admin/realms/telcocrm-gygy5/users/" + keycloakId)).build();
        when(keycloakAdminClient.createUser(any())).thenReturn(response);

        String result = keycloakSyncService.syncUserCreation(user);

        assertThat(result).isEqualTo(keycloakId);
        verify(keycloakAdminClient).createUser(userRepresentationCaptor.capture());
        var captured = userRepresentationCaptor.getValue();
        assertThat(captured.username()).isEqualTo("agokhan");
        assertThat(captured.email()).isEqualTo("agokhan@example.com");
        assertThat(captured.firstName()).isEqualTo("Ahmet");
        assertThat(captured.lastName()).isEqualTo("Gokhan");
        assertThat(captured.enabled()).isTrue();
    }

    @Test
    void syncUserCreation_shouldSplitSingleWordFullNameAsFirstNameOnly() {
        var user = User.builder().id(UUID.randomUUID()).username("cher").email("cher@example.com").fullName("Cher").status(UserStatus.ACTIVE).build();
        ResponseEntity<Void> response = ResponseEntity.created(URI.create("/admin/realms/telcocrm-gygy5/users/" + UUID.randomUUID())).build();
        when(keycloakAdminClient.createUser(any())).thenReturn(response);

        keycloakSyncService.syncUserCreation(user);

        verify(keycloakAdminClient).createUser(userRepresentationCaptor.capture());
        assertThat(userRepresentationCaptor.getValue().firstName()).isEqualTo("Cher");
        assertThat(userRepresentationCaptor.getValue().lastName()).isEqualTo("");
    }

    @Test
    void syncUserCreation_shouldThrowWhenLocationHeaderMissing() {
        var user = aUser();
        when(keycloakAdminClient.createUser(any())).thenReturn(ResponseEntity.ok().build());

        assertThatThrownBy(() -> keycloakSyncService.syncUserCreation(user))
                .isInstanceOf(KeycloakSyncException.class)
                .hasMessageContaining("agokhan");
    }

    @Test
    void syncUserCreationFallback_shouldWrapAnyFailureAsKeycloakSyncException() {
        var user = aUser();
        var cause = new RuntimeException("invalid_client");

        assertThatThrownBy(() -> keycloakSyncService.syncUserCreationFallback(user, cause))
                .isInstanceOf(KeycloakSyncException.class)
                .hasMessageContaining("agokhan")
                .hasMessageContaining("invalid_client");
    }

    @Test
    void syncRoleAssignment_shouldLookUpRoleThenAssignIt() {
        var role = new KeycloakRoleRepresentation(UUID.randomUUID().toString(), "FIELD_DEALER");
        when(keycloakAdminClient.getRealmRole("FIELD_DEALER")).thenReturn(role);
        var keycloakUserId = UUID.randomUUID().toString();

        keycloakSyncService.syncRoleAssignment(keycloakUserId, "FIELD_DEALER");

        verify(keycloakAdminClient).assignRealmRole(eq(keycloakUserId), eq(List.of(role)));
    }

    @Test
    void syncRoleAssignmentFallback_shouldWrapAnyFailureAsKeycloakSyncException() {
        var keycloakUserId = UUID.randomUUID().toString();
        var cause = new RuntimeException("403 Forbidden");

        assertThatThrownBy(() -> keycloakSyncService.syncRoleAssignmentFallback(keycloakUserId, "FIELD_DEALER", cause))
                .isInstanceOf(KeycloakSyncException.class)
                .hasMessageContaining("FIELD_DEALER")
                .hasMessageContaining(keycloakUserId)
                .hasMessageContaining("403 Forbidden");
    }
}
