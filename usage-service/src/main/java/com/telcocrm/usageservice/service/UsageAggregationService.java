package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.dto.response.AggregationRunResponse;
import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.event.publish.UsageAggregatedEvent;
import com.telcocrm.usageservice.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageAggregationService {

    private final QuotaRepository quotaRepository;
    private final OutboxService outboxService;

    @Transactional
    public AggregationRunResponse run(LocalDate asOf) {
        List<Quota> closedQuotas = quotaRepository.findByPeriodEndBeforeAndAggregatedAtIsNull(asOf);

        for (Quota quota : closedQuotas) {
            outboxService.saveEvent(
                    "QUOTA",
                    quota.getSubscriptionId().toString(),
                    "usage-aggregated-topic",
                    UsageAggregatedEvent.of(quota)
            );
            quota.setAggregatedAt(LocalDateTime.now());
        }

        log.info("Usage aggregation completed asOf: {} aggregated quotas: {}", asOf, closedQuotas.size());
        return new AggregationRunResponse(asOf, closedQuotas.size());
    }
}
