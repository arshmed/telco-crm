package com.telcocrm.productcatalogservice.controller;

import com.telcocrm.productcatalogservice.dto.request.TariffCreateRequest;
import com.telcocrm.productcatalogservice.dto.request.TariffPriceChangeRequest;
import com.telcocrm.productcatalogservice.dto.request.TariffUpdateRequest;
import com.telcocrm.productcatalogservice.dto.response.TariffResponse;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.service.TariffService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TariffControllerTest {

    private final TariffService tariffService = mock(TariffService.class);
    private final TariffController controller = new TariffController(tariffService);

    private TariffResponse response() {
        return new TariffResponse(null, "GNC-20GB", 1, true, "Genc 20GB", null, null,
                new BigDecimal("150.00"), "TRY", 500, 250, 20480, TariffStatus.DRAFT,
                LocalDate.now(), null, java.util.Set.of(), null, null);
    }

    @Test
    void createReturns201WithBody() {
        TariffResponse body = response();
        TariffCreateRequest request = new TariffCreateRequest("GNC-20GB", "Genc 20GB", null, null,
                new BigDecimal("150.00"), null, 500, 250, 20480, LocalDate.now(), null, null);
        when(tariffService.create(request)).thenReturn(body);

        ResponseEntity<TariffResponse> result = controller.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(body, result.getBody());
    }

    @Test
    void listDelegatesToService() {
        Page<TariffResponse> page = new PageImpl<>(List.of(response()));
        Pageable pageable = Pageable.ofSize(20);
        when(tariffService.list(TariffStatus.ACTIVE, pageable)).thenReturn(page);

        assertSame(page, controller.list(TariffStatus.ACTIVE, pageable));
    }

    @Test
    void getByCodeDelegatesToService() {
        TariffResponse body = response();
        when(tariffService.getByCode("GNC-20GB")).thenReturn(body);

        assertSame(body, controller.getByCode("GNC-20GB"));
    }

    @Test
    void getVersionsDelegatesToService() {
        List<TariffResponse> versions = List.of(response());
        when(tariffService.getVersions("GNC-20GB")).thenReturn(versions);

        assertSame(versions, controller.getVersions("GNC-20GB"));
    }

    @Test
    void getByCodeAndVersionDelegatesToService() {
        TariffResponse body = response();
        when(tariffService.getByCodeAndVersion("GNC-20GB", 2)).thenReturn(body);

        assertSame(body, controller.getByCodeAndVersion("GNC-20GB", 2));
    }

    @Test
    void updateDelegatesToService() {
        TariffResponse body = response();
        TariffUpdateRequest request = new TariffUpdateRequest("Genc 25GB", null,
                new BigDecimal("180.00"), null, 750, 250, 25600, null);
        when(tariffService.update("GNC-20GB", request)).thenReturn(body);

        assertSame(body, controller.update("GNC-20GB", request));
    }

    @Test
    void changePriceDelegatesToService() {
        TariffResponse body = response();
        TariffPriceChangeRequest request = new TariffPriceChangeRequest(new BigDecimal("99.00"));
        when(tariffService.changePrice(eq("GNC-20GB"), any(BigDecimal.class))).thenReturn(body);

        assertSame(body, controller.changePrice("GNC-20GB", request));
        verify(tariffService).changePrice("GNC-20GB", new BigDecimal("99.00"));
    }

    @Test
    void publishDelegatesToService() {
        TariffResponse body = response();
        when(tariffService.publish("GNC-20GB")).thenReturn(body);

        assertSame(body, controller.publish("GNC-20GB"));
    }

    @Test
    void deleteReturns204AndDelegates() {
        ResponseEntity<Void> result = controller.delete("GNC-20GB");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(tariffService).delete("GNC-20GB");
    }
}
