package com.telcocrm.customerservice.entity;

import com.telcocrm.customerservice.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityNumberConverterTest {

    @Mock
    private EncryptionService encryptionService;

    private IdentityNumberConverter converter;

    @BeforeEach
    void setUp() {
        converter = new IdentityNumberConverter(encryptionService);
    }

    @Test
    void shouldEncryptForDatabaseColumn() {
        when(encryptionService.encrypt("12345678901")).thenReturn("encrypted-value");
        assertThat(converter.convertToDatabaseColumn("12345678901")).isEqualTo("encrypted-value");
    }

    @Test
    void shouldDecryptForEntityAttribute() {
        when(encryptionService.decrypt("encrypted-value")).thenReturn("12345678901");
        assertThat(converter.convertToEntityAttribute("encrypted-value")).isEqualTo("12345678901");
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNullWhenNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_shouldReturnNullWhenNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_shouldReturnRawValueOnDecryptFailure() {
        when(encryptionService.decrypt("bad-cipher")).thenThrow(new RuntimeException("decrypt failed"));
        assertThat(converter.convertToEntityAttribute("bad-cipher")).isEqualTo("bad-cipher");
    }
}
