package com.telcocrm.billingservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ProductCatalogFallbackFactory implements FallbackFactory<ProductCatalogClient> {

    @Override
    public ProductCatalogClient create(Throwable cause) {
        log.error("Product catalog service fallback triggered", cause);
        return code -> {
            log.warn("Fallback: returning empty tariff for code: {}", code);
            return Map.of("code", code, "monthlyFee", 0);
        };
    }
}
