package com.telcocrm.productcatalogservice.controller;

import com.telcocrm.productcatalogservice.dto.request.TariffCreateRequest;
import com.telcocrm.productcatalogservice.dto.request.TariffPriceChangeRequest;
import com.telcocrm.productcatalogservice.dto.response.TariffResponse;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffResponse> create(@Valid @RequestBody TariffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffService.create(request));
    }

    @GetMapping
    public Page<TariffResponse> list(
            @RequestParam(required = false) TariffStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return tariffService.list(status, pageable);
    }

    @GetMapping("/{code}")
    public TariffResponse getByCode(@PathVariable String code) {
        return tariffService.getByCode(code);
    }

    @GetMapping("/{code}/versions")
    public List<TariffResponse> getVersions(@PathVariable String code) {
        return tariffService.getVersions(code);
    }

    @GetMapping("/{code}/versions/{version}")
    public TariffResponse getByCodeAndVersion(@PathVariable String code, @PathVariable Integer version) {
        return tariffService.getByCodeAndVersion(code, version);
    }

    @PatchMapping("/{code}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public TariffResponse changePrice(@PathVariable String code, @Valid @RequestBody TariffPriceChangeRequest request) {
        return tariffService.changePrice(code, request.monthlyFee());
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        tariffService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
