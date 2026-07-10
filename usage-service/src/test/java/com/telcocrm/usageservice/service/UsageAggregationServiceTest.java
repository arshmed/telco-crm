package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.event.publish.UsageAggregatedEvent;
import com.telcocrm.usageservice.repository.QuotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageAggregationServiceTest {

    @Mock
    private QuotaRepository quotaRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UsageAggregationService usageAggregationService;

    private Quota aClosedQuota() {
        return Quota.builder()
                .id(UUID.randomUUID())
                .subscriptionId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .msisdn("905551112233")
                .tariffCode("TARIFF-100")
                .periodStart(LocalDate.of(2026, 6, 1))
                .periodEnd(LocalDate.of(2026, 6, 30))
                .minutesIncluded(1000)
                .smsIncluded(500)
                .dataMbIncluded(20000)
                .minutesUsed(1100)
                .smsUsed(200)
                .dataMbUsed(19000)
                .build();
    }

    @Test
    void shouldAggregateClosedQuotasAndPublishEvents() {
        var asOf = LocalDate.of(2026, 7, 1);
        var quota = aClosedQuota();
        when(quotaRepository.findByPeriodEndBeforeAndAggregatedAtIsNull(asOf)).thenReturn(List.of(quota));

        var response = usageAggregationService.run(asOf);

        assertThat(response.asOf()).isEqualTo(asOf);
        assertThat(response.aggregatedCount()).isEqualTo(1);
        assertThat(quota.getAggregatedAt()).isNotNull();
        verify(outboxService).saveEvent(
                eq("QUOTA"), eq(quota.getSubscriptionId().toString()),
                eq("usage-aggregated-topic"), any(UsageAggregatedEvent.class));
    }

    @Test
    void shouldReturnZeroWhenNoClosedQuotas() {
        var asOf = LocalDate.of(2026, 7, 1);
        when(quotaRepository.findByPeriodEndBeforeAndAggregatedAtIsNull(asOf)).thenReturn(List.of());

        var response = usageAggregationService.run(asOf);

        assertThat(response.aggregatedCount()).isEqualTo(0);
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }
}
