package com.telcocrm.productcatalogservice.mapper;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import org.springframework.stereotype.Component;

@Component
public class AddonMapper {

    public Addon toEntity(AddonCreateRequest request) {
        return Addon.builder()
                .code(request.code())
                .name(request.name())
                .type(request.type())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "TRY")
                .validityDays(request.validityDays())
                .build();
    }

    public AddonResponse toResponse(Addon addon) {
        return new AddonResponse(
                addon.getId(),
                addon.getCode(),
                addon.getName(),
                addon.getType(),
                addon.getPrice(),
                addon.getCurrency(),
                addon.getValidityDays(),
                addon.getCreatedAt(),
                addon.getUpdatedAt()
        );
    }
}
