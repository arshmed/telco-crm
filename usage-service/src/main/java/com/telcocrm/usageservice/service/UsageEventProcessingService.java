package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.client.ProductCatalogClient;
import com.telcocrm.usageservice.client.dto.TariffResponse;
import com.telcocrm.usageservice.entity.ProcessedEvent;
import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.entity.UsageRecord;
import com.telcocrm.usageservice.entity.enums.UsageType;
import com.telcocrm.usageservice.event.consume.CdrRecordedEvent;
import com.telcocrm.usageservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.usageservice.event.publish.QuotaExceededEvent;
import com.telcocrm.usageservice.event.publish.QuotaThresholdReachedEvent;
import com.telcocrm.usageservice.repository.ProcessedEventRepository;
import com.telcocrm.usageservice.repository.QuotaRepository;
import com.telcocrm.usageservice.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEventProcessingService {

    private static final int WARNING_THRESHOLD_PERCENT = 80;

    private final QuotaRepository quotaRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProductCatalogClient productCatalogClient;
    private final OutboxService outboxService;

    @Transactional
    public void processSubscriptionActivated(SubscriptionActivatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("SubscriptionActivatedEvent already processed: {}", event.eventId());
            return;
        }

        TariffResponse tariff = productCatalogClient.getTariffByCode(event.tariffCode());

        LocalDate periodStart = event.occurredAt().toLocalDate();
        Quota quota = Quota.builder()
                .subscriptionId(event.subscriptionId())
                .customerId(event.customerId())
                .msisdn(event.msisdn())
                .tariffCode(event.tariffCode())
                .periodStart(periodStart)
                .periodEnd(periodStart.plusMonths(1).minusDays(1))
                .minutesIncluded(defaultZero(tariff.minutesIncluded()))
                .smsIncluded(defaultZero(tariff.smsIncluded()))
                .dataMbIncluded(defaultZero(tariff.dataMbIncluded()))
                .build();

        quotaRepository.save(quota);

        markProcessed(event.eventId());

        log.info("Quota created for subscriptionId: {} with tariff: {}", event.subscriptionId(), event.tariffCode());
    }

    @Transactional
    public void processCdrRecorded(CdrRecordedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.warn("CdrRecordedEvent already processed: {}", event.eventId());
            return;
        }

        Quota quota = quotaRepository.findActiveForUpdate(event.subscriptionId(), event.occurredAt().toLocalDate())
                .orElse(null);
        if (quota == null) {
            log.warn("No active quota for subscriptionId: {}, skipping CDR: {}", event.subscriptionId(), event.cdrRef());
            markProcessed(event.eventId());
            return;
        }

        usageRecordRepository.save(
                UsageRecord.builder()
                        .subscriptionId(event.subscriptionId())
                        .type(event.type())
                        .quantity(event.quantity())
                        .recordedAt(event.occurredAt())
                        .cdrRef(event.cdrRef())
                        .build()
        );

        int before;
        int included;
        switch (event.type()) {
            case VOICE -> {
                before = quota.getMinutesUsed();
                included = quota.getMinutesIncluded();
                quota.setMinutesUsed(before + event.quantity());
            }
            case SMS -> {
                before = quota.getSmsUsed();
                included = quota.getSmsIncluded();
                quota.setSmsUsed(before + event.quantity());
            }
            case DATA -> {
                before = quota.getDataMbUsed();
                included = quota.getDataMbIncluded();
                quota.setDataMbUsed(before + event.quantity());
            }
            default -> throw new IllegalStateException("Unknown usage type: " + event.type());
        }
        int after = before + event.quantity();

        publishThresholdEvents(quota, event.type(), before, after, included);

        markProcessed(event.eventId());

        log.info("CDR processed for subscriptionId: {} type: {} quantity: {}",
                event.subscriptionId(), event.type(), event.quantity());
    }

    private void publishThresholdEvents(Quota quota, UsageType type, int before, int after, int included) {
        if (included <= 0) {
            return;
        }

        boolean crossedWarning = percentOf(before, included) < WARNING_THRESHOLD_PERCENT
                && percentOf(after, included) >= WARNING_THRESHOLD_PERCENT;
        boolean crossedLimit = before < included && after >= included;

        if (crossedWarning) {
            outboxService.saveEvent("QUOTA", quota.getSubscriptionId().toString(), "quota-threshold-reached-topic",
                    QuotaThresholdReachedEvent.of(quota.getSubscriptionId(), quota.getCustomerId(), quota.getMsisdn(),
                            type, WARNING_THRESHOLD_PERCENT, after, included));
        }
        if (crossedLimit) {
            outboxService.saveEvent("QUOTA", quota.getSubscriptionId().toString(), "quota-exceeded-topic",
                    QuotaExceededEvent.of(quota.getSubscriptionId(), quota.getCustomerId(), quota.getMsisdn(),
                            type, after, included));
        }
    }

    private int percentOf(int used, int included) {
        return (int) (used * 100L / included);
    }

    private Integer defaultZero(Integer value) {
        return value != null ? value : 0;
    }

    private void markProcessed(UUID eventId) {
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .processedAt(Instant.now())
                        .build()
        );
    }
}
