package com.telcocrm.customerservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) i;
        encryptionService = new EncryptionService(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void shouldEncryptAndDecryptRoundtrip() {
        String plaintext = "12345678901";
        String encrypted = encryptionService.encrypt(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void shouldReturnNullForNullEncrypt() {
        assertThat(encryptionService.encrypt(null)).isNull();
    }

    @Test
    void shouldReturnNullForNullDecrypt() {
        assertThat(encryptionService.decrypt(null)).isNull();
    }

    @Test
    void shouldProduceDifferentCiphertextsForSamePlaintext() {
        String plaintext = "12345678901";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void shouldHandleLongStrings() {
        String longText = "A".repeat(1000);
        String encrypted = encryptionService.encrypt(longText);
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo(longText);
    }

    @Test
    void shouldThrowOnInvalidCiphertext() {
        assertThatThrownBy(() -> encryptionService.decrypt("invalid-base64!!"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldEncryptIdentityNumber() {
        String identityNumber = "12345678901";
        String encrypted = encryptionService.encrypt(identityNumber);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).doesNotContain(identityNumber);
        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(identityNumber);
    }

    @Test
    void shouldHandleSpecialCharacters() {
        String specialText = "ABC-123_XYZ+şğüöçıİ";
        String encrypted = encryptionService.encrypt(specialText);
        assertThat(encryptionService.decrypt(encrypted)).isEqualTo(specialText);
    }
}
