package com.telcocrm.usageservice.service;

import com.telcocrm.usageservice.dto.response.QuotaResponse;
import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.entity.UsageRecord;
import com.telcocrm.usageservice.entity.enums.UsageType;
import com.telcocrm.usageservice.exception.QuotaNotFoundException;
import com.telcocrm.usageservice.repository.QuotaRepository;
import com.telcocrm.usageservice.repository.UsageRecordRepository;
import com.telcocrm.usageservice.security.CustomerAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageQueryServiceTest {

    @Mock
    private QuotaRepository quotaRepository;

    @Mock
    private UsageRecordRepository usageRecordRepository;

    @Mock
    private CustomerAccessGuard customerAccessGuard;

    @InjectMocks
    private UsageQueryService usageQueryService;

    @Captor
    private ArgumentCaptor<LocalDateTime> fromCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> toCaptor;

    private Quota aQuota(UUID subscriptionId) {
        return Quota.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .customerId(UUID.randomUUID())
                .msisdn("905551112233")
                .tariffCode("TARIFF-100")
                .periodStart(LocalDate.of(2026, 7, 1))
                .periodEnd(LocalDate.of(2026, 7, 31))
                .minutesIncluded(1000)
                .smsIncluded(500)
                .dataMbIncluded(20000)
                .minutesUsed(200)
                .smsUsed(50)
                .dataMbUsed(5000)
                .build();
    }

    @Test
    void shouldReturnQuotaWhenActiveQuotaExists() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId);
        when(quotaRepository.findActive(eq(subscriptionId), any(LocalDate.class))).thenReturn(Optional.of(quota));

        QuotaResponse response = usageQueryService.getQuota(subscriptionId);

        assertThat(response.subscriptionId()).isEqualTo(subscriptionId);
        assertThat(response.minutesRemaining()).isEqualTo(800);
        assertThat(response.smsRemaining()).isEqualTo(450);
        assertThat(response.dataMbRemaining()).isEqualTo(15000);
    }

    @Test
    void shouldThrowWhenNoActiveQuota() {
        var subscriptionId = UUID.randomUUID();
        when(quotaRepository.findActive(eq(subscriptionId), any(LocalDate.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usageQueryService.getQuota(subscriptionId))
                .isInstanceOf(QuotaNotFoundException.class);
    }

    @Test
    void shouldThrowWhenCustomerAccessGuardDeniesQuota() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId);
        when(quotaRepository.findActive(eq(subscriptionId), any(LocalDate.class))).thenReturn(Optional.of(quota));
        org.mockito.Mockito.doThrow(new QuotaNotFoundException(subscriptionId))
                .when(customerAccessGuard).assertOwnResource(eq(quota.getCustomerId()), any());

        assertThatThrownBy(() -> usageQueryService.getQuota(subscriptionId))
                .isInstanceOf(QuotaNotFoundException.class);
    }

    @Test
    void shouldReturnHistoryForExplicitDateRange() {
        var subscriptionId = UUID.randomUUID();
        var from = LocalDateTime.of(2026, 7, 1, 0, 0);
        var to = LocalDateTime.of(2026, 7, 31, 23, 59);
        var pageable = PageRequest.of(0, 20);
        var record = UsageRecord.builder()
                .id(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .type(UsageType.DATA)
                .quantity(150)
                .recordedAt(LocalDateTime.of(2026, 7, 15, 10, 0))
                .cdrRef("cdr-1")
                .build();
        when(quotaRepository.findFirstBySubscriptionIdOrderByPeriodStartDesc(subscriptionId))
                .thenReturn(Optional.of(aQuota(subscriptionId)));
        when(usageRecordRepository.findBySubscriptionIdAndRecordedAtBetween(subscriptionId, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(record)));

        var page = usageQueryService.getHistory(subscriptionId, from, to, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).cdrRef()).isEqualTo("cdr-1");
    }

    @Test
    void shouldThrowWhenCustomerAccessGuardDeniesHistory() {
        var subscriptionId = UUID.randomUUID();
        var quota = aQuota(subscriptionId);
        var pageable = PageRequest.of(0, 20);
        when(quotaRepository.findFirstBySubscriptionIdOrderByPeriodStartDesc(subscriptionId))
                .thenReturn(Optional.of(quota));
        org.mockito.Mockito.doThrow(new QuotaNotFoundException(subscriptionId))
                .when(customerAccessGuard).assertOwnResource(eq(quota.getCustomerId()), any());

        assertThatThrownBy(() -> usageQueryService.getHistory(subscriptionId, null, null, pageable))
                .isInstanceOf(QuotaNotFoundException.class);
    }

    @Test
    void shouldPassNullResourceCustomerIdToGuardWhenNoQuotaRowExistsForSubscription() {
        var subscriptionId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        when(quotaRepository.findFirstBySubscriptionIdOrderByPeriodStartDesc(subscriptionId))
                .thenReturn(Optional.empty());
        when(usageRecordRepository.findBySubscriptionIdAndRecordedAtBetween(eq(subscriptionId), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        usageQueryService.getHistory(subscriptionId, null, null, pageable);

        org.mockito.Mockito.verify(customerAccessGuard).assertOwnResource(org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void shouldUseDefaultDateRangeWhenNotProvided() {
        var subscriptionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(usageRecordRepository.findBySubscriptionIdAndRecordedAtBetween(
                eq(subscriptionId), fromCaptor.capture(), toCaptor.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        usageQueryService.getHistory(subscriptionId, null, null, pageable);

        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
        assertThat(toCaptor.getValue()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
    }
}
