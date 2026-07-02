package com.telcocrm.orderservice.client;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import com.telcocrm.orderservice.exception.ProductNotFoundException;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "product-catalog-service", path = "/api/v1")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    @CircuitBreaker(name = "productCatalogServiceCircuitBreaker", fallbackMethod = "getTariffByCodeFallback")
    ProductResponse getTariffByCode(@PathVariable String code);

    @GetMapping("/addons/{code}")
    @CircuitBreaker(name = "productCatalogServiceCircuitBreaker", fallbackMethod = "getAddonByCodeFallback")
    ProductResponse getAddonByCode(@PathVariable String code);

    default ProductResponse getProductByCode(OrderItemType productType, String code) {
        return switch (productType) {
            case TARIFF -> getTariffByCode(code);
            case ADDON -> getAddonByCode(code);
            case VAS -> throw new UnsupportedOperationException("VAS products are not supported yet");
        };
    }

    default ProductResponse getTariffByCodeFallback(String code, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new ProductNotFoundException(code);
        }
        throw new ServiceUnavailableException("Product catalog service", "PRODUCT_CATALOG_SERVICE_UNAVAILABLE", throwable);
    }

    default ProductResponse getAddonByCodeFallback(String code, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new ProductNotFoundException(code);
        }
        throw new ServiceUnavailableException("Product catalog service", "PRODUCT_CATALOG_SERVICE_UNAVAILABLE", throwable);
    }
}