package com.telcocrm.billingservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "product-catalog-service")
public interface ProductCatalogClient {

    @GetMapping("/api/v1/tariffs/{code}")
    @CircuitBreaker(name = "productCatalogServiceCircuitBreaker", fallbackMethod = "getTariffFallback")
    Map<String, Object> getTariff(@PathVariable("code") String code);

    default Map<String, Object> getTariffFallback(String code, Throwable throwable) {
        return Map.of("code", code, "monthlyFee", 0);
    }
}
