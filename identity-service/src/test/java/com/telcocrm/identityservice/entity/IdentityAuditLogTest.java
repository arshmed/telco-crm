package com.telcocrm.identityservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAuditLogTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var entityId = UUID.randomUUID();
        var createdAt = LocalDateTime.now();

        var log = IdentityAuditLog.builder()
                .id(id)
                .entityType("USER")
                .entityId(entityId)
                .action("CREATED")
                .detail("User created: agokhan")
                .performedBy("SYSTEM")
                .createdAt(createdAt)
                .build();

        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getEntityType()).isEqualTo("USER");
        assertThat(log.getEntityId()).isEqualTo(entityId);
        assertThat(log.getAction()).isEqualTo("CREATED");
        assertThat(log.getDetail()).isEqualTo("User created: agokhan");
        assertThat(log.getPerformedBy()).isEqualTo("SYSTEM");
        assertThat(log.getCreatedAt()).isEqualTo(createdAt);
    }
}
