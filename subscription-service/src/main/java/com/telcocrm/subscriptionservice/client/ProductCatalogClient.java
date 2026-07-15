package com.telcocrm.subscriptionservice.client;

import com.telcocrm.subscriptionservice.client.dto.TariffResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", path = "/api/v1/catalog")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    TariffResponse getTariff(@PathVariable("code") String code);
}
