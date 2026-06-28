package com.telcocrm.orderservice.client;

import com.telcocrm.orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", path = "/api/v1")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    ProductResponse getProductByCode(@PathVariable String code);
}