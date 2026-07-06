package com.telcocrm.customerservice.config;

import com.telcocrm.customerservice.entity.AuditLogEntry;
import com.telcocrm.customerservice.entity.Customer;
import com.telcocrm.customerservice.enums.CustomerType;
import com.telcocrm.customerservice.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAuditListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private CustomerAuditListener listener;

    @Captor
    private ArgumentCaptor<AuditLogEntry> auditCaptor;

    @BeforeEach
    void setUp() {
        listener = new CustomerAuditListener(auditLogRepository);
    }

    private Customer aCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .type(CustomerType.INDIVIDUAL)
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void shouldLogCreate() {
        var customer = aCustomer();
        listener.logCreate("Customer", customer.getId().toString(), customer);
        verify(auditLogRepository).save(auditCaptor.capture());
        var entry = auditCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo("CREATE");
        assertThat(entry.getEntityType()).isEqualTo("Customer");
        assertThat(entry.getEntityId()).isEqualTo(customer.getId().toString());
        assertThat(entry.getNewValues()).isSameAs(customer);
        assertThat(entry.getOldValues()).isNull();
    }

    @Test
    void shouldLogUpdate() {
        var customer = aCustomer();
        listener.logUpdate("Customer", customer.getId().toString(), null, customer);
        verify(auditLogRepository).save(auditCaptor.capture());
        var entry = auditCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo("UPDATE");
        assertThat(entry.getEntityType()).isEqualTo("Customer");
        assertThat(entry.getEntityId()).isEqualTo(customer.getId().toString());
        assertThat(entry.getNewValues()).isSameAs(customer);
        assertThat(entry.getOldValues()).isNull();
    }

    @Test
    void shouldLogDelete() {
        var customer = aCustomer();
        listener.logDelete("Customer", customer.getId().toString(), customer);
        verify(auditLogRepository).save(auditCaptor.capture());
        var entry = auditCaptor.getValue();
        assertThat(entry.getAction()).isEqualTo("DELETE");
        assertThat(entry.getEntityType()).isEqualTo("Customer");
        assertThat(entry.getEntityId()).isEqualTo(customer.getId().toString());
        assertThat(entry.getOldValues()).isSameAs(customer);
        assertThat(entry.getNewValues()).isNull();
    }

    @Test
    void shouldSetChangedByToSystem() {
        var customer = aCustomer();
        listener.logCreate("Customer", customer.getId().toString(), customer);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getChangedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void shouldHandleRepositoryFailure() {
        doThrow(new RuntimeException("DB down"))
            .when(auditLogRepository).save(any());
        var customer = aCustomer();
        listener.logCreate("Customer", customer.getId().toString(), customer);
        verify(auditLogRepository).save(any());
    }

    @Test
    void shouldWorkWithNullId() {
        var customer = Customer.builder().build();
        listener.logCreate("Customer", null, customer);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEntityId()).isNull();
    }
}
