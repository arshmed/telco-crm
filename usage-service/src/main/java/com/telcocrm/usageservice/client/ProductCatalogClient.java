package com.telcocrm.usageservice.client;

import com.telcocrm.usageservice.client.dto.TariffResponse;
import com.telcocrm.usageservice.exception.DownstreamAccessException;
import com.telcocrm.usageservice.exception.ServiceUnavailableException;
import com.telcocrm.usageservice.exception.TariffNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", path = "/api/v1")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    @CircuitBreaker(name = "productCatalogServiceCircuitBreaker", fallbackMethod = "getTariffByCodeFallback")
    TariffResponse getTariffByCode(@PathVariable String code);

    default TariffResponse getTariffByCodeFallback(String code, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new TariffNotFoundException(code);
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
