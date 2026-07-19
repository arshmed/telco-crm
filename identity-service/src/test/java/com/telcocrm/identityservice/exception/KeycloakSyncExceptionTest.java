package com.telcocrm.identityservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakSyncExceptionTest {

    @Test
    void shouldBuildExceptionWithMessageAndStatus() {
        var ex = new KeycloakSyncException("Failed to sync user to Keycloak: agokhan");

        assertThat(ex.getMessage()).contains("agokhan");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getErrorCode()).isEqualTo("KEYCLOAK_SYNC_FAILED");
    }
}
