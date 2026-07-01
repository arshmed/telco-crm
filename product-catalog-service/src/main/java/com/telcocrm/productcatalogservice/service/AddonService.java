package com.telcocrm.productcatalogservice.service;

import com.telcocrm.productcatalogservice.dto.request.AddonCreateRequest;
import com.telcocrm.productcatalogservice.dto.response.AddonResponse;
import com.telcocrm.productcatalogservice.entity.Addon;
import com.telcocrm.productcatalogservice.exception.AddonNotFoundException;
import com.telcocrm.productcatalogservice.exception.DuplicateCodeException;
import com.telcocrm.productcatalogservice.mapper.AddonMapper;
import com.telcocrm.productcatalogservice.repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddonService {

    private final AddonRepository addonRepository;
    private final AddonMapper addonMapper;

    @Transactional
    public AddonResponse create(AddonCreateRequest request) {
        if (addonRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException(request.code());
        }
        Addon saved = addonRepository.save(addonMapper.toEntity(request));
        return addonMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AddonResponse getByCode(String code) {
        Addon addon = addonRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new AddonNotFoundException(code));
        return addonMapper.toResponse(addon);
    }

    @Transactional(readOnly = true)
    public List<AddonResponse> listAll() {
        return addonRepository.findByDeletedFalse().stream()
                .map(addonMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String code) {
        Addon addon = addonRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new AddonNotFoundException(code));
        addon.setDeleted(true);
    }
}
