package com.telcocrm.orderservice.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.telcocrm.orderservice.client.dto.CustomerResponse;

@FeignClient(name = "customer-service", path = "/api/v1")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    CustomerResponse getCustomerById(@PathVariable UUID id);

}
