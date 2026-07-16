package com.telcocrm.billingservice.client;

import com.telcocrm.billingservice.exception.DownstreamAccessException;
import com.telcocrm.billingservice.exception.ResourceNotFoundException;
import com.telcocrm.billingservice.exception.ServiceUnavailableException;
import feign.FeignException;
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
        if (throwable instanceof FeignException.NotFound) {
            throw new ResourceNotFoundException("Tariff", "code", code);
        }
        if (throwable instanceof FeignException.Unauthorized) {
            throw new DownstreamAccessException("Product catalog service", "JWT token rejected (401)", throwable);
        }
        if (throwable instanceof FeignException.Forbidden) {
            throw new DownstreamAccessException("Product catalog service", "insufficient permissions (403)", throwable);
        }
        throw new ServiceUnavailableException("Product catalog service", "PRODUCT_CATALOG_SERVICE_UNAVAILABLE", throwable);
    }
}
