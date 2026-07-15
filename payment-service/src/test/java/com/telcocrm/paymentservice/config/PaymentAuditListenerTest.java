package com.telcocrm.paymentservice.config;

import com.telcocrm.paymentservice.entity.AuditLogEntry;
import com.telcocrm.paymentservice.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAuditListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private PaymentAuditListener listener;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private PaymentAuditListener listener() {
        return new PaymentAuditListener(auditLogRepository);
    }

    @Test
    void logCreate_shouldSaveEntryWithCreateActionAndSystemActor() {
        listener = listener();

        listener.logCreate("Payment", "p-1", Map.of("status", "PENDING"));

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEntityType()).isEqualTo("Payment");
        assertThat(entry.getEntityId()).isEqualTo("p-1");
        assertThat(entry.getAction()).isEqualTo("CREATE");
        assertThat(entry.getChangedBy()).isEqualTo("SYSTEM");
        assertThat(entry.getOldValues()).isNull();
        assertThat(entry.getNewValues()).isEqualTo(Map.of("status", "PENDING"));
    }

    @Test
    void logUpdate_shouldCaptureAuthenticatedUserAsChangedBy() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("orhan", null, "ROLE_USER"));
        listener = listener();

        listener.logUpdate("Payment", "p-2", Map.of("status", "PENDING"), Map.of("status", "COMPLETED"));

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo("UPDATE");
        assertThat(entry.getChangedBy()).isEqualTo("orhan");
        assertThat(entry.getOldValues()).isEqualTo(Map.of("status", "PENDING"));
        assertThat(entry.getNewValues()).isEqualTo(Map.of("status", "COMPLETED"));
    }

    @Test
    void logDelete_shouldSaveEntryWithDeleteActionAndNullNewValues() {
        listener = listener();

        listener.logDelete("Payment", "p-3", Map.of("status", "FAILED"));

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo("DELETE");
        assertThat(entry.getNewValues()).isNull();
    }

    @Test
    void log_shouldSwallowExceptionWhenRepositorySaveFails() {
        listener = listener();
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.logCreate("Payment", "p-4", Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void logUpdate_shouldFallBackToSystemWhenUnauthenticated() {
        listener = listener();

        listener.logUpdate("Payment", "p-5", Map.of(), Map.of());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isEqualTo("SYSTEM");
    }
}
