package com.telcocrm.orderservice.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "customer-service", path = "/api/v1")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    @CircuitBreaker(name = "customerServiceCircuitBreaker", fallbackMethod = "getCustomerByIdFallback")
    CustomerResponse getCustomerById(@PathVariable UUID id);

    default CustomerResponse getCustomerByIdFallback(UUID id, Throwable throwable) {
        throw new ServiceUnavailableException("Customer service", "CUSTOMER_SERVICE_UNAVAILABLE", throwable);
    }

}
