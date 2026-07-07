package com.telcocrm.orderservice.client;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import com.telcocrm.orderservice.exception.ProductNotFoundException;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCatalogClientTest {

    private final ProductResponse tariffResponse =
            new ProductResponse("TARIFF-1", "Tariff One", BigDecimal.TEN, "ACTIVE", "TRY");
    private final ProductResponse addonResponse =
            new ProductResponse("ADDON-1", "Addon One", BigDecimal.ONE, "ACTIVE", "TRY");

    private final ProductCatalogClient client = new ProductCatalogClient() {
        @Override
        public ProductResponse getTariffByCode(String code) {
            return tariffResponse;
        }

        @Override
        public ProductResponse getAddonByCode(String code) {
            return addonResponse;
        }
    };

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/tariffs/1", Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void shouldDispatchToTariffLookupForTariffType() {
        ProductResponse result = client.getProductByCode(OrderItemType.TARIFF, "TARIFF-1");

        assertThat(result).isEqualTo(tariffResponse);
    }

    @Test
    void shouldDispatchToAddonLookupForAddonType() {
        ProductResponse result = client.getProductByCode(OrderItemType.ADDON, "ADDON-1");

        assertThat(result).isEqualTo(addonResponse);
    }

    @Test
    void shouldDispatchToAddonLookupForVasType() {
        ProductResponse result = client.getProductByCode(OrderItemType.VAS, "ADDON-1");

        assertThat(result).isEqualTo(addonResponse);
    }

    @Test
    void shouldThrowProductNotFoundWhenTariffFallbackReceives404() {
        var notFound = new FeignException.NotFound("not found", dummyRequest(), null, null);

        assertThatThrownBy(() -> client.getTariffByCodeFallback("TARIFF-1", notFound))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("TARIFF-1");
    }

    @Test
    void shouldThrowServiceUnavailableWhenTariffFallbackReceivesOtherFailure() {
        var cause = new RuntimeException("timeout");

        assertThatThrownBy(() -> client.getTariffByCodeFallback("TARIFF-1", cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Product catalog service");
    }

    @Test
    void shouldThrowProductNotFoundWhenAddonFallbackReceives404() {
        var notFound = new FeignException.NotFound("not found", dummyRequest(), null, null);

        assertThatThrownBy(() -> client.getAddonByCodeFallback("ADDON-1", notFound))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("ADDON-1");
    }

    @Test
    void shouldThrowServiceUnavailableWhenAddonFallbackReceivesOtherFailure() {
        var cause = new RuntimeException("timeout");

        assertThatThrownBy(() -> client.getAddonByCodeFallback("ADDON-1", cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Product catalog service");
    }
}
