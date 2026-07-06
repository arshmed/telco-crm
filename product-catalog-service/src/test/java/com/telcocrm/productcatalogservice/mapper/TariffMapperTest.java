package com.telcocrm.productcatalogservice.mapper;

import com.telcocrm.productcatalogservice.dto.request.TariffUpdateRequest;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TariffMapperTest {

    private final TariffMapper tariffMapper = new TariffMapper(new AddonMapper());

    @Test
    void newVersionAppliesUpdateRequestAndBumpsVersion() {
        Addon addon = Addon.builder().id(UUID.randomUUID()).code("EXTRA-5GB").build();
        Tariff current = Tariff.builder()
                .code("GNC-20GB")
                .version(3)
                .current(true)
                .name("Genc 20GB")
                .type(TariffType.POSTPAID)
                .segment(TariffSegment.YOUTH)
                .monthlyFee(new BigDecimal("150.00"))
                .currency("TRY")
                .minutesIncluded(500)
                .smsIncluded(250)
                .dataMbIncluded(20480)
                .status(TariffStatus.ACTIVE)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();
        TariffUpdateRequest request = new TariffUpdateRequest(
                "Genc 25GB", TariffSegment.YOUTH, new BigDecimal("180.00"),
                null, 750, 250, 25600, null);
        LocalDate today = LocalDate.of(2026, 7, 2);

        Tariff next = tariffMapper.newVersion(current, request, today, Set.of(addon));

        assertEquals("GNC-20GB", next.getCode());
        assertEquals(4, next.getVersion());
        assertTrue(next.isCurrent());
        assertEquals("Genc 25GB", next.getName());
        assertEquals(TariffType.POSTPAID, next.getType());
        assertEquals(new BigDecimal("180.00"), next.getMonthlyFee());
        assertEquals("TRY", next.getCurrency());
        assertEquals(750, next.getMinutesIncluded());
        assertEquals(25600, next.getDataMbIncluded());
        assertEquals(TariffStatus.ACTIVE, next.getStatus());
        assertEquals(today, next.getEffectiveFrom());
        assertNull(next.getEffectiveTo());
        assertEquals(Set.of(addon), next.getAddons());
    }
}
