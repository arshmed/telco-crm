package com.telcocrm.productcatalogservice.mapper;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddonMapperTest {

    private final AddonMapper mapper = new AddonMapper();

    @Test
    void toEntityDefaultsCurrencyToTryWhenAbsent() {
        AddonCreateRequest request = new AddonCreateRequest(
                "EXTRA-5GB", "Ekstra 5GB", AddonType.DATA, new BigDecimal("50.00"), null, 30);

        Addon entity = mapper.toEntity(request);

        assertEquals("EXTRA-5GB", entity.getCode());
        assertEquals("Ekstra 5GB", entity.getName());
        assertEquals(AddonType.DATA, entity.getType());
        assertEquals(new BigDecimal("50.00"), entity.getPrice());
        assertEquals("TRY", entity.getCurrency());
        assertEquals(30, entity.getValidityDays());
    }

    @Test
    void toEntityKeepsProvidedCurrency() {
        AddonCreateRequest request = new AddonCreateRequest(
                "EXTRA-5GB", "Ekstra 5GB", AddonType.DATA, new BigDecimal("50.00"), "USD", 30);

        assertEquals("USD", mapper.toEntity(request).getCurrency());
    }

    @Test
    void toResponseCopiesAllFields() {
        UUID id = UUID.randomUUID();
        Addon addon = Addon.builder()
                .id(id).code("EXTRA-5GB").name("Ekstra 5GB").type(AddonType.DATA)
                .price(new BigDecimal("50.00")).currency("TRY").validityDays(30)
                .build();

        AddonResponse response = mapper.toResponse(addon);

        assertEquals(id, response.id());
        assertEquals("EXTRA-5GB", response.code());
        assertEquals("Ekstra 5GB", response.name());
        assertEquals(AddonType.DATA, response.type());
        assertEquals(new BigDecimal("50.00"), response.price());
        assertEquals("TRY", response.currency());
        assertEquals(30, response.validityDays());
    }
}
