package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.dto.response.QuotaResponse;
import com.telcocrm.usageservice.dto.response.UsageRecordResponse;
import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.exception.QuotaNotFoundException;
import com.telcocrm.usageservice.repository.QuotaRepository;
import com.telcocrm.usageservice.repository.UsageRecordRepository;
import com.telcocrm.usageservice.security.CustomerAccessGuard;
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
    private final CustomerAccessGuard customerAccessGuard;

    @Transactional(readOnly = true)
    public QuotaResponse getQuota(UUID subscriptionId) {
        Quota quota = quotaRepository.findActive(subscriptionId, LocalDate.now())
                .orElseThrow(() -> new QuotaNotFoundException(subscriptionId));
        customerAccessGuard.assertOwnResource(quota.getCustomerId(), () -> new QuotaNotFoundException(subscriptionId));
        return QuotaResponse.from(quota);
    }

    @Transactional(readOnly = true)
    public Page<UsageRecordResponse> getHistory(UUID subscriptionId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        UUID resourceCustomerId = quotaRepository.findFirstBySubscriptionIdOrderByPeriodStartDesc(subscriptionId)
                .map(Quota::getCustomerId)
                .orElse(null);
        customerAccessGuard.assertOwnResource(resourceCustomerId, () -> new QuotaNotFoundException(subscriptionId));

        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        return usageRecordRepository
                .findBySubscriptionIdAndRecordedAtBetween(subscriptionId, effectiveFrom, effectiveTo, pageable)
                .map(UsageRecordResponse::from);
    }
}
