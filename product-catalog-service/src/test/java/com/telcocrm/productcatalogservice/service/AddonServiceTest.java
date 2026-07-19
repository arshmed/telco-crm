package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.request.AddonUpdateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import com.telcocrm.productcatalogservice.exception.AddonNotFoundException;
import com.telcocrm.productcatalogservice.exception.DuplicateCodeException;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void createPersistsNewAddon() {
        AddonCreateRequest request = new AddonCreateRequest(
                "EXTRA-5GB", "Ekstra 5GB", AddonType.DATA, new BigDecimal("50.00"), null, 30);
        when(addonRepository.existsByCode("EXTRA-5GB")).thenReturn(false);
        when(addonRepository.save(any(Addon.class))).thenAnswer(inv -> inv.getArgument(0));

        AddonResponse response = addonService.create(request);

        assertEquals("EXTRA-5GB", response.code());
        assertEquals("TRY", response.currency());
        verify(addonRepository).save(any(Addon.class));
    }

    @Test
    void createRejectsDuplicateCode() {
        when(addonRepository.existsByCode("EXTRA-5GB")).thenReturn(true);

        assertThrows(DuplicateCodeException.class, () -> addonService.create(
                new AddonCreateRequest("EXTRA-5GB", "Ekstra 5GB", AddonType.DATA, BigDecimal.ONE, null, 30)));
    }

    @Test
    void getByCodeReturnsMappedAddon() {
        Addon addon = Addon.builder().id(UUID.randomUUID()).code("EXTRA-5GB").name("Ekstra 5GB")
                .type(AddonType.DATA).price(new BigDecimal("50.00")).currency("TRY").validityDays(30).build();
        when(addonRepository.findByCodeAndDeletedFalse("EXTRA-5GB")).thenReturn(Optional.of(addon));

        assertEquals("EXTRA-5GB", addonService.getByCode("EXTRA-5GB").code());
    }

    @Test
    void getByCodeFailsForUnknownCode() {
        when(addonRepository.findByCodeAndDeletedFalse("YOK")).thenReturn(Optional.empty());

        assertThrows(AddonNotFoundException.class, () -> addonService.getByCode("YOK"));
    }

    @Test
    void listAllReturnsMappedAddons() {
        Addon addon = Addon.builder().id(UUID.randomUUID()).code("EXTRA-5GB").type(AddonType.DATA).build();
        when(addonRepository.findByDeletedFalse()).thenReturn(List.of(addon));

        List<AddonResponse> result = addonService.listAll();

        assertEquals(1, result.size());
        assertEquals("EXTRA-5GB", result.get(0).code());
    }

    @Test
    void deleteMarksAddonDeleted() {
        Addon addon = Addon.builder().id(UUID.randomUUID()).code("EXTRA-5GB").build();
        when(addonRepository.findByCodeAndDeletedFalse("EXTRA-5GB")).thenReturn(Optional.of(addon));

        addonService.delete("EXTRA-5GB");

        assertTrue(addon.isDeleted());
    }

    @Test
    void deleteFailsForUnknownCode() {
        when(addonRepository.findByCodeAndDeletedFalse("YOK")).thenReturn(Optional.empty());

        assertThrows(AddonNotFoundException.class, () -> addonService.delete("YOK"));
    }
}
