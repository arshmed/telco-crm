package com.telcocrm.subscriptionservice.client;

import com.telcocrm.subscriptionservice.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service", path = "/api/v1/customers")
public interface CustomerClient {

    @GetMapping("/{id}")
    CustomerResponse getCustomer(@PathVariable("id") UUID id);
}
