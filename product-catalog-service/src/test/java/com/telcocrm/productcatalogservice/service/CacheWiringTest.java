package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.mapper.TariffMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import com.telcocrm.productcatalogservice.repository.TariffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TariffService.class, AddonService.class, CacheWiringTest.CacheTestConfig.class})
class CacheWiringTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @MockitoBean
    private TariffRepository tariffRepository;
    @MockitoBean
    private AddonRepository addonRepository;
    @MockitoBean
    private TariffMapper tariffMapper;
    @MockitoBean
    private AddonMapper addonMapper;
    @MockitoBean
    private OutboxService outboxService;

    @Autowired
    private TariffService tariffService;
    @Autowired
    private AddonService addonService;

    @Test
    void tariffGetByCodeIsCached() {
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(tariff));

        tariffService.getByCode("GNC-20GB");
        tariffService.getByCode("GNC-20GB");

        verify(tariffRepository, times(1)).findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB");
    }

    @Test
    void priceChangeEvictsTariffCache() {
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("VIP-50GB").build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("VIP-50GB")).thenReturn(Optional.of(tariff));
        Tariff next = Tariff.builder().id(UUID.randomUUID()).code("VIP-50GB").version(2).build();
        when(tariffMapper.newVersion(eq(tariff), any(BigDecimal.class), any(LocalDate.class))).thenReturn(next);
        when(tariffRepository.save(next)).thenReturn(next);

        tariffService.getByCode("VIP-50GB");
        tariffService.changePrice("VIP-50GB", new BigDecimal("99.00"));
        tariffService.getByCode("VIP-50GB");

        verify(tariffRepository, times(3)).findByCodeAndDeletedFalseAndCurrentTrue("VIP-50GB");
    }

    @Test
    void addonListIsCachedUntilCreate() {
        when(addonRepository.findByDeletedFalse()).thenReturn(List.of());

        addonService.listAll();
        addonService.listAll();
        verify(addonRepository, times(1)).findByDeletedFalse();

        Addon saved = Addon.builder().id(UUID.randomUUID()).code("YENI").build();
        when(addonRepository.existsByCode("YENI")).thenReturn(false);
        when(addonMapper.toEntity(any())).thenReturn(saved);
        when(addonRepository.save(saved)).thenReturn(saved);
        addonService.create(new AddonCreateRequest("YENI", "Yeni Paket", AddonType.DATA, BigDecimal.ONE, null, null));

        addonService.listAll();
        verify(addonRepository, times(2)).findByDeletedFalse();
    }
}
