package com.telcocrm.paymentservice.client;

import com.telcocrm.paymentservice.client.dto.OrderResponse;
import com.telcocrm.paymentservice.exception.DownstreamAccessException;
import com.telcocrm.paymentservice.exception.OrderNotFoundException;
import com.telcocrm.paymentservice.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", path = "/api/v1")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "getOrderByIdFallback")
    OrderResponse getOrderById(@PathVariable UUID id);

    default OrderResponse getOrderByIdFallback(UUID id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new OrderNotFoundException(id);
        }
        if (throwable instanceof FeignException.Unauthorized) {
            throw new DownstreamAccessException("Order service", "JWT token rejected (401)", throwable);
        }
        if (throwable instanceof FeignException.Forbidden) {
            throw new DownstreamAccessException("Order service", "insufficient permissions (403)", throwable);
        }
        throw new ServiceUnavailableException("Order service", "ORDER_SERVICE_UNAVAILABLE", throwable);
    }
}
