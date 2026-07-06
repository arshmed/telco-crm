package com.telcocrm.customerservice.entity;

import com.telcocrm.customerservice.enums.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var doc = Document.builder()
                .id(id)
                .type(DocumentType.ID_CARD)
                .fileRef("ref-123")
                .build();
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getType()).isEqualTo(DocumentType.ID_CARD);
        assertThat(doc.getFileRef()).isEqualTo("ref-123");
    }
}
