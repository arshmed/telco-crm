package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.TariffCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.dto.response.TariffResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.event.publish.TariffCreatedEvent;
import com.telcocrm.productcatalogservice.event.publish.TariffPriceChangedEvent;
import com.telcocrm.productcatalogservice.exception.AddonNotFoundException;
import com.telcocrm.productcatalogservice.exception.DuplicateCodeException;
import com.telcocrm.productcatalogservice.exception.TariffNotFoundException;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.mapper.TariffMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import com.telcocrm.productcatalogservice.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AddonRepository addonRepository;
    private final TariffMapper tariffMapper;
    private final AddonMapper addonMapper;
    private final OutboxService outboxService;

    @Transactional
    public TariffResponse create(TariffCreateRequest request) {
        if (tariffRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException(request.code());
        }
        Set<Addon> addons = resolveAddons(request.addonCodes());
        Tariff saved = tariffRepository.save(tariffMapper.toEntity(request, addons));
        outboxService.publish("Tariff", saved.getId().toString(), "TariffCreated", TariffCreatedEvent.of(saved));
        return tariffMapper.toResponse(saved);
    }

    @Transactional
    public TariffResponse changePrice(String code, BigDecimal newMonthlyFee) {
        Tariff current = tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue(code)
                .orElseThrow(() -> new TariffNotFoundException(code));
        BigDecimal oldMonthlyFee = current.getMonthlyFee();
        LocalDate today = LocalDate.now();

        current.setCurrent(false);
        current.setEffectiveTo(today);

        Tariff nextVersion = tariffRepository.save(tariffMapper.newVersion(current, newMonthlyFee, today));

        outboxService.publish("Tariff", nextVersion.getId().toString(), "TariffPriceChanged",
                TariffPriceChangedEvent.of(nextVersion.getId(), nextVersion.getCode(), nextVersion.getVersion(),
                        oldMonthlyFee, newMonthlyFee, nextVersion.getCurrency()));
        return tariffMapper.toResponse(nextVersion);
    }

    @Transactional(readOnly = true)
    public TariffResponse getByCode(String code) {
        Tariff tariff = tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue(code)
                .orElseThrow(() -> new TariffNotFoundException(code));
        return tariffMapper.toResponse(tariff);
    }

    @Transactional(readOnly = true)
    public TariffResponse getByCodeAndVersion(String code, Integer version) {
        Tariff tariff = tariffRepository.findByCodeAndVersionAndDeletedFalse(code, version)
                .orElseThrow(() -> new TariffNotFoundException(code));
        return tariffMapper.toResponse(tariff);
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getVersions(String code) {
        List<Tariff> versions = tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc(code);
        if (versions.isEmpty()) {
            throw new TariffNotFoundException(code);
        }
        return versions.stream()
                .map(tariffMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TariffResponse> list(TariffStatus status, Pageable pageable) {
        Page<Tariff> page = (status == null)
                ? tariffRepository.findByDeletedFalseAndCurrentTrue(pageable)
                : tariffRepository.findByStatusAndDeletedFalseAndCurrentTrue(status, pageable);
        return page.map(tariffMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AddonResponse> getAddons(String tariffCode) {
        Tariff tariff = tariffRepository.findByCodeAndDeletedFalseAndCurrentTrue(tariffCode)
                .orElseThrow(() -> new TariffNotFoundException(tariffCode));
        return tariff.getAddons().stream()
                .map(addonMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String code) {
        List<Tariff> versions = tariffRepository.findByCodeAndDeletedFalseOrderByVersionDesc(code);
        if (versions.isEmpty()) {
            throw new TariffNotFoundException(code);
        }
        versions.forEach(tariff -> {
            tariff.setDeleted(true);
            tariff.setCurrent(false);
        });
    }

    private Set<Addon> resolveAddons(Set<String> addonCodes) {
        Set<Addon> addons = new HashSet<>();
        if (addonCodes == null || addonCodes.isEmpty()) {
            return addons;
        }
        for (String code : addonCodes) {
            Addon addon = addonRepository.findByCodeAndDeletedFalse(code)
                    .orElseThrow(() -> new AddonNotFoundException(code));
            addons.add(addon);
        }
        return addons;
    }
}
