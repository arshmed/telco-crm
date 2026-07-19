package com.telcocrm.usageservice.client;

import com.telcocrm.usageservice.client.dto.CustomerResponse;
import com.telcocrm.usageservice.exception.DownstreamAccessException;
import com.telcocrm.usageservice.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service", path = "/api/v1")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    @CircuitBreaker(name = "customerServiceCircuitBreaker", fallbackMethod = "getCustomerByIdFallback")
    CustomerResponse getCustomerById(@PathVariable UUID id);

    default CustomerResponse getCustomerByIdFallback(UUID id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            return null;
        }
        if (throwable instanceof FeignException.Unauthorized) {
            throw new DownstreamAccessException("Customer service", "JWT token rejected (401)", throwable);
        }
        if (throwable instanceof FeignException.Forbidden) {
            throw new DownstreamAccessException("Customer service", "insufficient permissions (403)", throwable);
        }
        throw new ServiceUnavailableException("Customer service", "CUSTOMER_SERVICE_UNAVAILABLE", throwable);
    }
}
