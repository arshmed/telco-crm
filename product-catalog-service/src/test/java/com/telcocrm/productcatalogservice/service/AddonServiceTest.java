package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.AddonUpdateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import com.telcocrm.productcatalogservice.exception.AddonNotFoundException;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddonServiceTest {

    private final AddonRepository addonRepository = mock(AddonRepository.class);
    private final AddonService addonService = new AddonService(addonRepository, new AddonMapper());

    @Test
    void updateChangesFieldsInPlace() {
        Addon addon = Addon.builder()
                .id(UUID.randomUUID())
                .code("EXTRA-5GB")
                .name("Ekstra 5GB")
                .type(AddonType.DATA)
                .price(new BigDecimal("50.00"))
                .currency("TRY")
                .validityDays(30)
                .build();
        when(addonRepository.findByCodeAndDeletedFalse("EXTRA-5GB")).thenReturn(Optional.of(addon));
        AddonUpdateRequest request = new AddonUpdateRequest(
                "Ekstra 5GB Plus", new BigDecimal("60.00"), null, 45);

        AddonResponse response = addonService.update("EXTRA-5GB", request);

        assertEquals("Ekstra 5GB Plus", addon.getName());
        assertEquals(new BigDecimal("60.00"), addon.getPrice());
        assertEquals("TRY", addon.getCurrency());
        assertEquals(45, addon.getValidityDays());
        assertEquals(AddonType.DATA, addon.getType());
        assertEquals("Ekstra 5GB Plus", response.name());
    }

    @Test
    void updateFailsForUnknownCode() {
        when(addonRepository.findByCodeAndDeletedFalse("YOK")).thenReturn(Optional.empty());

        assertThrows(AddonNotFoundException.class,
                () -> addonService.update("YOK", new AddonUpdateRequest("x", BigDecimal.ONE, null, null)));
    }
}
