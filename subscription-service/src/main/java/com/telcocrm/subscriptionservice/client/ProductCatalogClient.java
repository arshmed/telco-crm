package com.telcocrm.subscriptionservice.client;

import com.telcocrm.subscriptionservice.client.dto.AddonResponse;
import com.telcocrm.subscriptionservice.client.dto.TariffResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// path'in "/api/v1" olması gerekiyor: product-catalog-service'in gerçek endpoint'leri
// /api/v1/tariffs ve /api/v1/addons altında, /api/v1/catalog diye bir prefix yok.
@FeignClient(name = "product-catalog-service", path = "/api/v1")
public interface ProductCatalogClient {

    @GetMapping("/tariffs/{code}")
    TariffResponse getTariff(@PathVariable("code") String code);

    @GetMapping("/addons/{code}")
    AddonResponse getAddon(@PathVariable("code") String code);
}
