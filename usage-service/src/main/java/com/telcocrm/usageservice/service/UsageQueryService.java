package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.dto.response.QuotaResponse;
import com.telcocrm.usageservice.dto.response.UsageRecordResponse;
import com.telcocrm.usageservice.exception.QuotaNotFoundException;
import com.telcocrm.usageservice.repository.QuotaRepository;
import com.telcocrm.usageservice.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsageQueryService {

    private final QuotaRepository quotaRepository;
    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true)
    public QuotaResponse getQuota(UUID subscriptionId) {
        return quotaRepository.findActive(subscriptionId, LocalDate.now())
                .map(QuotaResponse::from)
                .orElseThrow(() -> new QuotaNotFoundException(subscriptionId));
    }

    @Transactional(readOnly = true)
    public Page<UsageRecordResponse> getHistory(UUID subscriptionId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        return usageRecordRepository
                .findBySubscriptionIdAndRecordedAtBetween(subscriptionId, effectiveFrom, effectiveTo, pageable)
                .map(UsageRecordResponse::from);
    }
}
