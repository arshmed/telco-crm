package com.telcocrm.productcatalogservice.mapper;

import com.telcocrm.productcatalogservice.dto.request.TariffCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.dto.response.TariffResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TariffMapper {

    private final AddonMapper addonMapper;

    public Tariff toEntity(TariffCreateRequest request, Set<Addon> addons) {
        return Tariff.builder()
                .code(request.code())
                .version(1)
                .current(true)
                .name(request.name())
                .type(request.type())
                .segment(request.segment())
                .monthlyFee(request.monthlyFee())
                .currency(request.currency() != null ? request.currency() : "TRY")
                .minutesIncluded(request.minutesIncluded())
                .smsIncluded(request.smsIncluded())
                .dataMbIncluded(request.dataMbIncluded())
                .status(TariffStatus.DRAFT)
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .addons(addons)
                .build();
    }

    public Tariff newVersion(Tariff current, BigDecimal newMonthlyFee, LocalDate effectiveFrom) {
        return Tariff.builder()
                .code(current.getCode())
                .version(current.getVersion() + 1)
                .current(true)
                .name(current.getName())
                .type(current.getType())
                .segment(current.getSegment())
                .monthlyFee(newMonthlyFee)
                .currency(current.getCurrency())
                .minutesIncluded(current.getMinutesIncluded())
                .smsIncluded(current.getSmsIncluded())
                .dataMbIncluded(current.getDataMbIncluded())
                .status(current.getStatus())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .addons(new HashSet<>(current.getAddons()))
                .build();
    }

    public TariffResponse toResponse(Tariff tariff) {
        Set<AddonResponse> addons = tariff.getAddons().stream()
                .map(addonMapper::toResponse)
                .collect(Collectors.toSet());

        return new TariffResponse(
                tariff.getId(),
                tariff.getCode(),
                tariff.getVersion(),
                tariff.isCurrent(),
                tariff.getName(),
                tariff.getType(),
                tariff.getSegment(),
                tariff.getMonthlyFee(),
                tariff.getCurrency(),
                tariff.getMinutesIncluded(),
                tariff.getSmsIncluded(),
                tariff.getDataMbIncluded(),
                tariff.getStatus(),
                tariff.getEffectiveFrom(),
                tariff.getEffectiveTo(),
                addons,
                tariff.getCreatedAt(),
                tariff.getUpdatedAt()
        );
    }
}
