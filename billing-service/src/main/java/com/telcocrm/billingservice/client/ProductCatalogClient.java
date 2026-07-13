package com.telcocrm.billingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "product-catalog-service", fallbackFactory = ProductCatalogFallbackFactory.class)
public interface ProductCatalogClient {

    @GetMapping("/api/v1/tariffs/{code}")
    Map<String, Object> getTariff(@PathVariable("code") String code);
}
