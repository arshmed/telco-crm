package com.telcocrm.customerservice.entity;

import com.telcocrm.customerservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Converter
@RequiredArgsConstructor
public class IdentityNumberConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return encryptionService.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            return encryptionService.decrypt(ciphertext);
        } catch (Exception e) {
            log.warn("Failed to decrypt identity_number, returning raw value: {}", e.getMessage());
            return ciphertext;
        }
    }
}
