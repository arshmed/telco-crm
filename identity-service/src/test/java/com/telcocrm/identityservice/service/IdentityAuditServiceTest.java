package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.entity.IdentityAuditLog;
import com.telcocrm.identityservice.repository.IdentityAuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdentityAuditServiceTest {

    @Mock
    private IdentityAuditLogRepository identityAuditLogRepository;

    @InjectMocks
    private IdentityAuditService identityAuditService;

    @Captor
    private ArgumentCaptor<IdentityAuditLog> auditLogCaptor;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecordPerformedByFromAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("service-account-order-service", null, List.of()));
        var entityId = UUID.randomUUID();

        identityAuditService.log("USER", entityId, "CREATED", "User created: agokhan");

        verify(identityAuditLogRepository).save(auditLogCaptor.capture());
        IdentityAuditLog saved = auditLogCaptor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("USER");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getAction()).isEqualTo("CREATED");
        assertThat(saved.getDetail()).isEqualTo("User created: agokhan");
        assertThat(saved.getPerformedBy()).isEqualTo("service-account-order-service");
    }

    @Test
    void shouldFallBackToSystemWhenNoAuthentication() {
        var entityId = UUID.randomUUID();

        identityAuditService.log("ROLE", entityId, "CREATED", "Role created");

        verify(identityAuditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void shouldFallBackToSystemWhenAuthenticationIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        var entityId = UUID.randomUUID();

        identityAuditService.log("USER", entityId, "CREATED", "User created");

        verify(identityAuditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void resolvePerformedBy_shouldReturnAuthenticatedUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agokhan", null, List.of()));

        assertThat(identityAuditService.resolvePerformedBy()).isEqualTo("agokhan");
    }

    @Test
    void resolvePerformedBy_shouldReturnSystemWhenNoAuthentication() {
        assertThat(identityAuditService.resolvePerformedBy()).isEqualTo("SYSTEM");
    }
}
