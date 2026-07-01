package com.telcocrm.orderservice.client;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "product-catalog-service", path = "/api/v1")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    @CircuitBreaker(name = "productCatalogServiceCircuitBreaker", fallbackMethod = "getProductByCodeFallback")
    ProductResponse getProductByCode(@PathVariable String code);

    default ProductResponse getProductByCodeFallback(String code, Throwable throwable) {
        throw new ServiceUnavailableException("Product catalog service", "PRODUCT_CATALOG_SERVICE_UNAVAILABLE", throwable);
    }
}