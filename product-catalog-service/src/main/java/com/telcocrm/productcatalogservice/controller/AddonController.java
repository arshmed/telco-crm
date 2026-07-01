package com.telcocrm.productcatalogservice.controller;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.service.AddonService;
import com.telcocrm.productcatalogservice.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addons")
@RequiredArgsConstructor
public class AddonController {

    private final AddonService addonService;
    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddonResponse> create(@Valid @RequestBody AddonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonService.create(request));
    }

    @GetMapping
    public List<AddonResponse> list(@RequestParam(required = false) String tariffCode) {
        if (tariffCode != null) {
            return tariffService.getAddons(tariffCode);
        }
        return addonService.listAll();
    }

    @GetMapping("/{code}")
    public AddonResponse getByCode(@PathVariable String code) {
        return addonService.getByCode(code);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        addonService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
