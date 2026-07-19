package com.telcocrm.productcatalogservice.controller;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.request.AddonUpdateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.enums.AddonType;
import com.telcocrm.productcatalogservice.service.AddonService;
import com.telcocrm.productcatalogservice.service.TariffService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AddonControllerTest {

    private final AddonService addonService = mock(AddonService.class);
    private final TariffService tariffService = mock(TariffService.class);
    private final AddonController controller = new AddonController(addonService, tariffService);

    private AddonResponse response() {
        return new AddonResponse(null, "EXTRA-5GB", "Ekstra 5GB", AddonType.DATA,
                new BigDecimal("50.00"), "TRY", 30, null, null);
    }

    @Test
    void createReturns201WithBody() {
        AddonResponse body = response();
        AddonCreateRequest request = new AddonCreateRequest("EXTRA-5GB", "Ekstra 5GB",
                AddonType.DATA, new BigDecimal("50.00"), null, 30);
        when(addonService.create(request)).thenReturn(body);

        ResponseEntity<AddonResponse> result = controller.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(body, result.getBody());
    }

    @Test
    void listWithoutTariffCodeReturnsAllAddons() {
        List<AddonResponse> all = List.of(response());
        when(addonService.listAll()).thenReturn(all);

        assertSame(all, controller.list(null));
        verifyNoInteractions(tariffService);
    }

    @Test
    void listWithTariffCodeDelegatesToTariffService() {
        List<AddonResponse> forTariff = List.of(response());
        when(tariffService.getAddons("GNC-20GB")).thenReturn(forTariff);

        assertSame(forTariff, controller.list("GNC-20GB"));
        verifyNoInteractions(addonService);
    }

    @Test
    void getByCodeDelegatesToService() {
        AddonResponse body = response();
        when(addonService.getByCode("EXTRA-5GB")).thenReturn(body);

        assertSame(body, controller.getByCode("EXTRA-5GB"));
    }

    @Test
    void updateDelegatesToService() {
        AddonResponse body = response();
        AddonUpdateRequest request = new AddonUpdateRequest("Ekstra 5GB Plus", new BigDecimal("60.00"), null, 45);
        when(addonService.update("EXTRA-5GB", request)).thenReturn(body);

        assertSame(body, controller.update("EXTRA-5GB", request));
    }

    @Test
    void deleteReturns204AndDelegates() {
        ResponseEntity<Void> result = controller.delete("EXTRA-5GB");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(addonService).delete("EXTRA-5GB");
    }
}
