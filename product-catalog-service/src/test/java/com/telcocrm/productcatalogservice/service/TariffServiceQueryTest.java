package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.TariffCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.dto.response.TariffResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;
import com.telcocrm.productcatalogservice.exception.AddonNotFoundException;
import com.telcocrm.productcatalogservice.exception.DuplicateCodeException;
import com.telcocrm.productcatalogservice.exception.TariffNotFoundException;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.mapper.TariffMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import com.telcocrm.productcatalogservice.repository.TariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceQueryTest {

    @Mock
    private TariffRepository tariffRepository;
    @Mock
    private AddonRepository addonRepository;
    @Mock
    private TariffMapper tariffMapper;
    @Mock
    private AddonMapper addonMapper;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TariffService tariffService;

    private TariffCreateRequest createRequest(Set<String> addonCodes) {
        return new TariffCreateRequest("GNC-20GB", "Genc 20GB", TariffType.POSTPAID, null,
                new BigDecimal("150.00"), null, 500, 250, 20480, LocalDate.now(), null, addonCodes);
    }

    @Test
    void createPersistsAndPublishesTariffCreated() {
        TariffCreateRequest request = createRequest(null);
        Tariff entity = Tariff.builder().code("GNC-20GB").build();
        Tariff saved = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").build();
        when(tariffRepository.existsByCode("GNC-20GB")).thenReturn(false);
        when(tariffMapper.toEntity(eq(request), any())).thenReturn(entity);
        when(tariffRepository.save(entity)).thenReturn(saved);

        tariffService.create(request);

        verify(outboxService).publish(eq("Tariff"), eq(saved.getId().toString()), eq("TariffCreated"), any());
        verify(tariffMapper).toResponse(saved);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(tariffRepository.existsByCode("GNC-20GB")).thenReturn(true);

        assertThrows(DuplicateCodeException.class, () -> tariffService.create(createRequest(null)));
        verifyNoInteractions(outboxService);
    }

    @Test
    void createResolvesAddonsAndFailsWhenAddonMissing() {
        when(tariffRepository.existsByCode("GNC-20GB")).thenReturn(false);
        when(addonRepository.findByCodeAndDeletedFalse("YOK")).thenReturn(Optional.empty());

        assertThrows(AddonNotFoundException.class, () -> tariffService.create(createRequest(Set.of("YOK"))));
    }

    @Test
    void getByCodeFailsForUnknownCode() {
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("YOK")).thenReturn(Optional.empty());

        assertThrows(TariffNotFoundException.class, () -> tariffService.getByCode("YOK"));
    }

    @Test
    void getByCodeAndVersionReturnsMappedResponse() {
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(2).build();
        TariffResponse response = mock(TariffResponse.class);
        when(tariffRepository.findByCodeAndVersionAndDeletedFalse("GNC-20GB", 2)).thenReturn(Optional.of(tariff));
        when(tariffMapper.toResponse(tariff)).thenReturn(response);

        assertSame(response, tariffService.getByCodeAndVersion("GNC-20GB", 2));
    }

    @Test
    void getByCodeAndVersionFailsWhenAbsent() {
        when(tariffRepository.findByCodeAndVersionAndDeletedFalse("GNC-20GB", 9)).thenReturn(Optional.empty());

        assertThrows(TariffNotFoundException.class, () -> tariffService.getByCodeAndVersion("GNC-20GB", 9));
    }

    @Test
    void getVersionsReturnsMappedListAndFailsWhenEmpty() {
        Tariff v1 = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(1).build();
        when(tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc("GNC-20GB")).thenReturn(List.of(v1));
        tariffService.getVersions("GNC-20GB");
        verify(tariffMapper).toResponse(v1);

        when(tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc("YOK")).thenReturn(List.of());
        assertThrows(TariffNotFoundException.class, () -> tariffService.getVersions("YOK"));
    }

    @Test
    void listUsesStatusFilterWhenProvidedOtherwiseAll() {
        Pageable pageable = Pageable.ofSize(20);
        Page<Tariff> page = new PageImpl<>(List.of());

        when(tariffRepository.findByDeletedFalseAndCurrentTrue(pageable)).thenReturn(page);
        tariffService.list(null, pageable);
        verify(tariffRepository).findByDeletedFalseAndCurrentTrue(pageable);

        when(tariffRepository.findByStatusAndDeletedFalseAndCurrentTrue(TariffStatus.ACTIVE, pageable)).thenReturn(page);
        tariffService.list(TariffStatus.ACTIVE, pageable);
        verify(tariffRepository).findByStatusAndDeletedFalseAndCurrentTrue(TariffStatus.ACTIVE, pageable);
    }

    @Test
    void getAddonsReturnsMappedAddonsOfTariff() {
        Addon addon = Addon.builder().id(UUID.randomUUID()).code("EXTRA-5GB").type(AddonType.DATA).build();
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").addons(Set.of(addon)).build();
        AddonResponse response = mock(AddonResponse.class);
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(tariff));
        when(addonMapper.toResponse(addon)).thenReturn(response);

        List<AddonResponse> result = tariffService.getAddons("GNC-20GB");

        assertEquals(List.of(response), result);
    }

    @Test
    void getAddonsFailsForUnknownTariff() {
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("YOK")).thenReturn(Optional.empty());

        assertThrows(TariffNotFoundException.class, () -> tariffService.getAddons("YOK"));
    }

    @Test
    void deleteSoftDeletesAllVersions() {
        Tariff v1 = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(1).current(true).build();
        Tariff v2 = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(2).current(true).build();
        when(tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc("GNC-20GB")).thenReturn(List.of(v2, v1));

        tariffService.delete("GNC-20GB");

        assertTrue(v1.isDeleted());
        assertTrue(v2.isDeleted());
    }

    @Test
    void deleteFailsWhenNoVersions() {
        when(tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc("YOK")).thenReturn(List.of());

        assertThrows(TariffNotFoundException.class, () -> tariffService.delete("YOK"));
    }

    @Test
    void changePricePublishesTariffPriceChanged() {
        Tariff current = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(1)
                .monthlyFee(new BigDecimal("150.00")).currency("TRY").build();
        Tariff next = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(2)
                .monthlyFee(new BigDecimal("99.00")).currency("TRY").build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(current));
        when(tariffMapper.newVersion(eq(current), eq(new BigDecimal("99.00")), any(LocalDate.class))).thenReturn(next);
        when(tariffRepository.save(next)).thenReturn(next);

        tariffService.changePrice("GNC-20GB", new BigDecimal("99.00"));

        assertEquals(LocalDate.now(), current.getEffectiveTo());
        verify(outboxService).publish(eq("Tariff"), eq(next.getId().toString()), eq("TariffPriceChanged"), any());
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
