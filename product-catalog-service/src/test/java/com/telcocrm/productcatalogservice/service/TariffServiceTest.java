package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.TariffUpdateRequest;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.exception.InvalidTariffStatusException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

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

    @Test
    void publishActivatesDraftTariff() {
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(1).build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(tariff));

        tariffService.publish("GNC-20GB");

        assertEquals(TariffStatus.ACTIVE, tariff.getStatus());
        verify(outboxService).publish(eq("Tariff"), eq(tariff.getId().toString()), eq("TariffPublished"), any());
    }

    @Test
    void publishRejectsNonDraftTariff() {
        Tariff tariff = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").status(TariffStatus.ACTIVE).build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(tariff));

        assertThrows(InvalidTariffStatusException.class, () -> tariffService.publish("GNC-20GB"));
        verifyNoInteractions(outboxService);
    }

    @Test
    void publishFailsForUnknownCode() {
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("YOK")).thenReturn(Optional.empty());

        assertThrows(TariffNotFoundException.class, () -> tariffService.publish("YOK"));
    }

    @Test
    void updateClosesOldVersionAndPublishesTariffUpdated() {
        Tariff current = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(1).build();
        when(tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue("GNC-20GB")).thenReturn(Optional.of(current));
        TariffUpdateRequest request = new TariffUpdateRequest(
                "Genc 25GB", TariffSegment.YOUTH, new BigDecimal("180.00"),
                null, 750, 250, 25600, null);
        Tariff next = Tariff.builder().id(UUID.randomUUID()).code("GNC-20GB").version(2).build();
        when(tariffMapper.newVersion(eq(current), eq(request), any(LocalDate.class), any())).thenReturn(next);
        when(tariffRepository.save(next)).thenReturn(next);

        tariffService.update("GNC-20GB", request);

        assertFalse(current.isCurrent());
        assertEquals(LocalDate.now(), current.getEffectiveTo());
        verify(outboxService).publish(eq("Tariff"), eq(next.getId().toString()), eq("TariffUpdated"), any());
    }
}
