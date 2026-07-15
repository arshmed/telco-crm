package com.telcocrm.subscriptionservice.service;

import com.telcocrm.subscriptionservice.client.CustomerClient;
import com.telcocrm.subscriptionservice.client.ProductCatalogClient;
import com.telcocrm.subscriptionservice.client.dto.CustomerResponse;
import com.telcocrm.subscriptionservice.client.dto.TariffResponse;
import com.telcocrm.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.telcocrm.subscriptionservice.dto.response.SubscriptionResponse;
import com.telcocrm.subscriptionservice.entity.Subscription;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import com.telcocrm.subscriptionservice.exception.InvalidStateTransitionException;
import com.telcocrm.subscriptionservice.exception.MsisdnAlreadyInUseException;
import com.telcocrm.subscriptionservice.exception.SubscriptionNotFoundException;
import com.telcocrm.subscriptionservice.mapper.SubscriptionMapper;
import com.telcocrm.subscriptionservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Subscription subscription;
    private SubscriptionResponse subscriptionResponse;

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerNo("C-000001")
                .msisdn("5551234567")
                .tariffCode("TARIFE-001")
                .status(SubscriptionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();

        subscriptionResponse = SubscriptionResponse.builder()
                .id(subscription.getId())
                .customerId(subscription.getCustomerId())
                .customerNo(subscription.getCustomerNo())
                .msisdn(subscription.getMsisdn())
                .tariffCode(subscription.getTariffCode())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    @Test
    void createSubscription_success() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .customerId(subscription.getCustomerId())
                .tariffCode("TARIFE-001")
                .msisdn("5551234567")
                .build();

        when(customerClient.getCustomer(request.getCustomerId())).thenReturn(mock(CustomerResponse.class));
        when(productCatalogClient.getTariff("TARIFE-001")).thenReturn(mock(TariffResponse.class));
        when(subscriptionRepository.findByMsisdn("5551234567")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(subscription)).thenReturn(subscriptionResponse);

        SubscriptionResponse result = subscriptionService.createSubscription(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscription_msisdnAlreadyInUse_throwsException() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .customerId(subscription.getCustomerId())
                .tariffCode("TARIFE-001")
                .msisdn("5551234567")
                .build();

        when(customerClient.getCustomer(request.getCustomerId())).thenReturn(mock(CustomerResponse.class));
        when(productCatalogClient.getTariff("TARIFE-001")).thenReturn(mock(TariffResponse.class));
        when(subscriptionRepository.findByMsisdn("5551234567")).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.createSubscription(request))
                .isInstanceOf(MsisdnAlreadyInUseException.class);
    }

    @Test
    void activateSubscription_success() {
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        subscriptionService.activateSubscription(subscription.getId());

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(outboxService).saveEvent(eq("SUBSCRIPTION"), eq(subscription.getId().toString()),
                eq("subscription-activated-topic"), any());
    }

    @Test
    void activateSubscription_invalidTransition_throwsException() {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.activateSubscription(subscription.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void createSubscriptionFromOrder_success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(subscriptionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        subscriptionService.createSubscriptionFromOrder(orderId, customerId, "TARIFE-001");

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createSubscriptionFromOrder_alreadyExists_skipsDuplicateCreation() {
        UUID orderId = UUID.randomUUID();
        subscription.setOrderId(orderId);
        when(subscriptionRepository.findByOrderId(orderId)).thenReturn(Optional.of(subscription));
        when(subscriptionMapper.toResponse(subscription)).thenReturn(subscriptionResponse);

        subscriptionService.createSubscriptionFromOrder(orderId, subscription.getCustomerId(), "TARIFE-001");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void activateSubscriptionFromPayment_success() {
        UUID orderId = UUID.randomUUID();
        subscription.setOrderId(orderId);
        when(subscriptionRepository.findByOrderId(orderId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        subscriptionService.activateSubscriptionFromPayment(orderId);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(outboxService).saveEvent(eq("SUBSCRIPTION"), eq(subscription.getId().toString()),
                eq("subscription-activated-topic"), any());
        verify(outboxService, never()).saveEvent(any(), any(), eq("subscription-activation-failed-topic"), any());
    }

    @Test
    void activateSubscriptionFromPayment_noSubscriptionForOrder_throwsForRetry() {
        UUID orderId = UUID.randomUUID();
        when(subscriptionRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // order-created-topic henüz işlenmemiş olabilir (race condition) — Kafka'nın retry etmesi için
        // exception fırlatılmalı, sessizce yutulmamalı.
        assertThatThrownBy(() -> subscriptionService.activateSubscriptionFromPayment(orderId))
                .isInstanceOf(IllegalStateException.class);

        verify(subscriptionRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void activateSubscriptionFromPayment_invalidTransition_publishesActivationFailedEvent() {
        UUID orderId = UUID.randomUUID();
        subscription.setOrderId(orderId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByOrderId(orderId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        subscriptionService.activateSubscriptionFromPayment(orderId);

        verify(outboxService).saveEvent(eq("SUBSCRIPTION"), eq(subscription.getId().toString()),
                eq("subscription-activation-failed-topic"), any());
    }

    @Test
    void suspendSubscription_success() {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        subscriptionService.suspendSubscription(subscription.getId());

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void terminateSubscription_fromSuspended_success() {
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        subscriptionService.terminateSubscription(subscription.getId());

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void getSubscription_notFound_throwsException() {
        when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscription(UUID.randomUUID()))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void reactivateSubscription_success() {
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toResponse(any())).thenReturn(subscriptionResponse);

        SubscriptionResponse result = subscriptionService.reactivateSubscription(subscription.getId());

        assertThat(result).isNotNull();
        verify(subscriptionRepository).save(any(Subscription.class));
    }
}
