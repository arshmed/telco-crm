package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.client.CustomerClient;
import com.telcocrm.usageservice.client.ProductCatalogClient;
import com.telcocrm.usageservice.client.dto.CustomerResponse;
import com.telcocrm.usageservice.client.dto.TariffResponse;
import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.entity.enums.UsageType;
import com.telcocrm.usageservice.event.consume.CdrRecordedEvent;
import com.telcocrm.usageservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.usageservice.event.publish.QuotaExceededEvent;
import com.telcocrm.usageservice.event.publish.QuotaThresholdReachedEvent;
import com.telcocrm.usageservice.repository.ProcessedEventRepository;
import com.telcocrm.usageservice.repository.QuotaRepository;
import com.telcocrm.usageservice.repository.UsageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageEventProcessingServiceTest {

    @Mock
    private QuotaRepository quotaRepository;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UsageEventProcessingService processingService;

    @Captor
    private ArgumentCaptor<Quota> quotaCaptor;

    private Quota aQuota(UUID subscriptionId, int minutesIncluded, int minutesUsed) {
        return Quota.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .customerId(UUID.randomUUID())
                .msisdn("905551112233")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .tariffCode("TARIFF-100")
                .periodStart(LocalDate.of(2026, 7, 1))
                .periodEnd(LocalDate.of(2026, 7, 31))
                .minutesIncluded(minutesIncluded)
                .smsIncluded(500)
                .dataMbIncluded(20000)
                .minutesUsed(minutesUsed)
                .build();
    }

    // ---- processSubscriptionActivated ----

    @Test
    void shouldCreateQuotaOnSubscriptionActivated() {
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 10, 0),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "905551112233", "TARIFF-100");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(productCatalogClient.getTariffByCode("TARIFF-100"))
                .thenReturn(new TariffResponse(UUID.randomUUID(), "TARIFF-100", 1000, 500, 20000));
        when(customerClient.getCustomerById(event.customerId()))
                .thenReturn(new CustomerResponse(event.customerId(), "ACTIVE", "test@example.com", "John", "Doe"));

        processingService.processSubscriptionActivated(event);

        verify(quotaRepository).save(quotaCaptor.capture());
        Quota saved = quotaCaptor.getValue();
        assertThat(saved.getSubscriptionId()).isEqualTo(event.subscriptionId());
        assertThat(saved.getPeriodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(saved.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(saved.getMinutesIncluded()).isEqualTo(1000);
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldCreateQuotaWithNullContactInfoWhenCustomerNotFound() {
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 10, 0),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "905551112233", "TARIFF-100");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(productCatalogClient.getTariffByCode("TARIFF-100"))
                .thenReturn(new TariffResponse(UUID.randomUUID(), "TARIFF-100", 1000, 500, 20000));
        when(customerClient.getCustomerById(event.customerId())).thenReturn(null);

        processingService.processSubscriptionActivated(event);

        verify(quotaRepository).save(quotaCaptor.capture());
        assertThat(quotaCaptor.getValue().getEmail()).isNull();
    }

    @Test
    void shouldSkipSubscriptionActivatedWhenAlreadyProcessed() {
        var event = new SubscriptionActivatedEvent(UUID.randomUUID(), LocalDateTime.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "905551112233", "TARIFF-100");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processSubscriptionActivated(event);

        verify(quotaRepository, never()).save(any());
        verify(productCatalogClient, never()).getTariffByCode(any());
    }

    // ---- processCdrRecorded ----

    @Test
    void shouldRecordUsageWithoutCrossingAnyThreshold() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId, 100, 50);
        var event = new CdrRecordedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 15, 10, 0),
                subscriptionId, UsageType.VOICE, 10, "cdr-1");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(quotaRepository.findActiveForUpdate(subscriptionId, event.occurredAt().toLocalDate()))
                .thenReturn(Optional.of(quota));

        processingService.processCdrRecorded(event);

        assertThat(quota.getMinutesUsed()).isEqualTo(60);
        verify(usageRecordRepository).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldPublishThresholdReachedWhenCrossing80Percent() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId, 100, 70);
        var event = new CdrRecordedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 15, 10, 0),
                subscriptionId, UsageType.VOICE, 15, "cdr-2");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(quotaRepository.findActiveForUpdate(subscriptionId, event.occurredAt().toLocalDate()))
                .thenReturn(Optional.of(quota));

        processingService.processCdrRecorded(event);

        assertThat(quota.getMinutesUsed()).isEqualTo(85);
        verify(outboxService).saveEvent(eq("QUOTA"), eq(subscriptionId.toString()),
                eq("quota-threshold-reached-topic"), any(QuotaThresholdReachedEvent.class));
        verify(outboxService, never()).saveEvent(any(), any(), eq("quota-exceeded-topic"), any());
    }

    @Test
    void shouldPublishQuotaExceededWhenCrossing100Percent() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId, 100, 90);
        var event = new CdrRecordedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 15, 10, 0),
                subscriptionId, UsageType.VOICE, 15, "cdr-3");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(quotaRepository.findActiveForUpdate(subscriptionId, event.occurredAt().toLocalDate()))
                .thenReturn(Optional.of(quota));

        processingService.processCdrRecorded(event);

        assertThat(quota.getMinutesUsed()).isEqualTo(105);
        verify(outboxService).saveEvent(eq("QUOTA"), eq(subscriptionId.toString()),
                eq("quota-exceeded-topic"), any(QuotaExceededEvent.class));
        verify(outboxService, never()).saveEvent(any(), any(), eq("quota-threshold-reached-topic"), any());
    }

    @Test
    void shouldSkipRecordingWhenNoActiveQuota() {
        var subscriptionId = UUID.randomUUID();
        var event = new CdrRecordedEvent(UUID.randomUUID(), LocalDateTime.of(2026, 7, 15, 10, 0),
                subscriptionId, UsageType.SMS, 1, "cdr-4");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(quotaRepository.findActiveForUpdate(subscriptionId, event.occurredAt().toLocalDate()))
                .thenReturn(Optional.empty());

        processingService.processCdrRecorded(event);

        verify(usageRecordRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldSkipCdrRecordedWhenAlreadyProcessed() {
        var event = new CdrRecordedEvent(UUID.randomUUID(), LocalDateTime.now(),
                UUID.randomUUID(), UsageType.SMS, 1, "cdr-5");
        when(processedEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        processingService.processCdrRecorded(event);

        verify(quotaRepository, never()).findActiveForUpdate(any(), any());
        verify(usageRecordRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }
}
