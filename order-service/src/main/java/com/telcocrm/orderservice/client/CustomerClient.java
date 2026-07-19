package com.telcocrm.orderservice.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.exception.CustomerNotFoundException;
import com.telcocrm.orderservice.exception.DownstreamAccessException;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;
import feign.FeignException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "customer-service", path = "/api/v1")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    @CircuitBreaker(name = "customerServiceCircuitBreaker", fallbackMethod = "getCustomerByIdFallback")
    CustomerResponse getCustomerById(@PathVariable UUID id);

    @GetMapping("/customers/byNo/{customerNo}")
    @CircuitBreaker(name = "customerServiceCircuitBreaker", fallbackMethod = "getCustomerByNoFallback")
    CustomerResponse getCustomerByNo(@PathVariable String customerNo);

    default CustomerResponse getCustomerByIdFallback(UUID id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new CustomerNotFoundException(id);
        }
        if (throwable instanceof FeignException.Unauthorized) {
            throw new DownstreamAccessException("Customer service", "JWT token rejected (401)", throwable);
        }
        if (throwable instanceof FeignException.Forbidden) {
            throw new DownstreamAccessException("Customer service", "insufficient permissions (403)", throwable);
        }
        throw new ServiceUnavailableException("Customer service", "CUSTOMER_SERVICE_UNAVAILABLE", throwable);
    }

    default CustomerResponse getCustomerByNoFallback(String customerNo, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new IllegalArgumentException("Customer not found with number: " + customerNo);
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
