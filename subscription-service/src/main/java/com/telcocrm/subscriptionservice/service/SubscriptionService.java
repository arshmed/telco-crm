package com.telcocrm.subscriptionservice.service;

import com.telcocrm.subscriptionservice.client.CustomerClient;
import com.telcocrm.subscriptionservice.client.ProductCatalogClient;
import com.telcocrm.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.telcocrm.subscriptionservice.dto.response.MonthlyActivationResponse;
import com.telcocrm.subscriptionservice.dto.response.SubscriptionResponse;
import com.telcocrm.subscriptionservice.dto.response.SubscriptionStatsResponse;
import com.telcocrm.subscriptionservice.dto.response.TariffDistributionResponse;
import com.telcocrm.subscriptionservice.entity.Subscription;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import com.telcocrm.subscriptionservice.exception.InvalidStateTransitionException;
import com.telcocrm.subscriptionservice.exception.MsisdnAlreadyInUseException;
import com.telcocrm.subscriptionservice.exception.SubscriptionNotFoundException;
import com.telcocrm.subscriptionservice.mapper.SubscriptionMapper;
import com.telcocrm.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OutboxService outboxService;
    private final CustomerClient customerClient;
    private final ProductCatalogClient productCatalogClient;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        customerClient.getCustomer(request.getCustomerId());
        productCatalogClient.getTariff(request.getTariffCode());

        if (request.getMsisdn() != null && !request.getMsisdn().isBlank()) {
            Optional<Subscription> existing = subscriptionRepository.findByMsisdn(request.getMsisdn());
            if (existing.isPresent()) {
                throw new MsisdnAlreadyInUseException("MSISDN " + request.getMsisdn() + " is already in use");
            }
        }

        Subscription subscription = Subscription.builder()
                .customerId(request.getCustomerId())
                .tariffCode(request.getTariffCode())
                .msisdn(request.getMsisdn() != null ? request.getMsisdn() : allocateMsisdn())
                .status(SubscriptionStatus.PENDING)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created: {} for customer: {}", saved.getId(), request.getCustomerId());

        return subscriptionMapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse activateSubscription(UUID id) {
        Subscription subscription = getSubscriptionEntity(id);
        validateTransition(subscription, SubscriptionStatus.ACTIVE);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActivatedAt(LocalDateTime.now());

        if (subscription.getMsisdn() == null) {
            subscription.setMsisdn(allocateMsisdn());
        }

        Subscription saved = subscriptionRepository.save(subscription);

        outboxService.saveEvent(
                "SUBSCRIPTION",
                saved.getId().toString(),
                "subscription-activated-topic",
                buildActivatedEvent(saved)
        );

        log.info("Subscription activated: {}", saved.getId());
        return subscriptionMapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse suspendSubscription(UUID id) {
        Subscription subscription = getSubscriptionEntity(id);
        validateTransition(subscription, SubscriptionStatus.SUSPENDED);

        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscription.setSuspendedAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription suspended: {}", saved.getId());
        return subscriptionMapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse reactivateSubscription(UUID id) {
        Subscription subscription = getSubscriptionEntity(id);
        validateTransition(subscription, SubscriptionStatus.ACTIVE);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSuspendedAt(null);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription reactivated: {}", saved.getId());
        return subscriptionMapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse terminateSubscription(UUID id) {
        Subscription subscription = getSubscriptionEntity(id);
        validateTransition(subscription, SubscriptionStatus.TERMINATED);

        subscription.setStatus(SubscriptionStatus.TERMINATED);
        subscription.setTerminatedAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription terminated: {}", saved.getId());
        return subscriptionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID id) {
        Subscription subscription = getSubscriptionEntity(id);
        return subscriptionMapper.toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable)
                .map(subscriptionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptionsByCustomer(UUID customerId, Pageable pageable) {
        return subscriptionRepository.findByCustomerId(customerId, pageable)
                .map(subscriptionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptionsByStatus(SubscriptionStatus status, Pageable pageable) {
        return subscriptionRepository.findByStatus(status, pageable)
                .map(subscriptionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatsResponse getStats() {
        long active = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long suspended = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);
        long pending = subscriptionRepository.countByStatus(SubscriptionStatus.PENDING);
        long terminated = subscriptionRepository.countByStatus(SubscriptionStatus.TERMINATED);
        return new SubscriptionStatsResponse(active, suspended, pending, terminated, active + suspended + pending + terminated);
    }

    @Transactional(readOnly = true)
    public List<MonthlyActivationResponse> getMonthlyActivations(int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Object[]> rows = subscriptionRepository.countMonthlyActivations(since);
        return rows.stream()
                .map(r -> new MonthlyActivationResponse((String) r[0], (Long) r[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TariffDistributionResponse> getTariffDistribution() {
        List<Object[]> rows = subscriptionRepository.countByTariffCode(SubscriptionStatus.ACTIVE);
        return rows.stream()
                .map(r -> new TariffDistributionResponse((String) r[0], (Long) r[1]))
                .toList();
    }

    @Transactional
    public SubscriptionResponse createSubscriptionFromOrder(UUID orderId, UUID customerId, String tariffCode) {
        // Kafka redelivery/retry senaryosunda (ör. bir önceki deneme DB hatasıyla rollback olduysa)
        // aynı sipariş için ikinci bir Subscription satırı oluşturulmasını engeller.
        Optional<Subscription> existing = subscriptionRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Subscription already exists for order {}, skipping duplicate creation", orderId);
            return subscriptionMapper.toResponse(existing.get());
        }

        Subscription subscription = Subscription.builder()
                .orderId(orderId)
                .customerId(customerId)
                .tariffCode(tariffCode)
                .msisdn(allocateMsisdn())
                .status(SubscriptionStatus.PENDING)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} created from order {} for customer {}", saved.getId(), orderId, customerId);

        return subscriptionMapper.toResponse(saved);
    }

    /**
     * payment-completed-topic tetiklediğinde çağrılır: order'a ait PENDING aboneliği otomatik aktive eder.
     * Aktivasyon başarısız olursa subscription-activation-failed-topic'e compensation event'i yayınlar
     * (payment-service bu event'i dinleyip ödemeyi otomatik iade eder).
     */
    @Transactional
    public void activateSubscriptionFromPayment(UUID orderId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByOrderId(orderId);
        if (subscriptionOpt.isEmpty()) {
            // order-created-topic ve payment-completed-topic bağımsız consumer thread'lerinde işleniyor,
            // sıralama garantisi yok. Abonelik satırı henüz yazılmamış olabilir (geçici durum) —
            // burada exception fırlatıp Kafka'nın kendi retry/backoff mekanizmasının kısa süre sonra
            // tekrar denemesini sağlıyoruz, aksi halde bu event sessizce kaybolur ve sipariş asla
            // AWAITING_SUBSCRIPTION'dan çıkamaz.
            throw new IllegalStateException("No subscription found yet for orderId: " + orderId
                    + " (likely OrderCreatedEvent not processed yet, will retry)");
        }

        Subscription subscription = subscriptionOpt.get();
        try {
            activateSubscription(subscription.getId());
        } catch (RuntimeException ex) {
            log.error("Subscription activation failed for orderId: {}: {}", orderId, ex.getMessage());
            outboxService.saveEvent(
                    "SUBSCRIPTION",
                    subscription.getId().toString(),
                    "subscription-activation-failed-topic",
                    buildActivationFailedEvent(orderId, ex.getMessage())
            );
        }
    }

    @Transactional
    public void handleOrderCancelled(UUID orderId) {
        subscriptionRepository.findByOrderId(orderId)
                .filter(s -> s.getStatus() == SubscriptionStatus.PENDING || s.getStatus() == SubscriptionStatus.ACTIVE)
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.TERMINATED);
                    sub.setTerminatedAt(LocalDateTime.now());
                    subscriptionRepository.save(sub);
                    log.info("Subscription {} terminated due to cancellation of order {}", sub.getId(), orderId);
                });
    }

    private Subscription getSubscriptionEntity(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found with id: " + id));
    }

    private void validateTransition(Subscription subscription, SubscriptionStatus targetStatus) {
        SubscriptionStatus current = subscription.getStatus();
        boolean valid = switch (current) {
            case PENDING -> targetStatus == SubscriptionStatus.ACTIVE;
            case ACTIVE -> targetStatus == SubscriptionStatus.SUSPENDED || targetStatus == SubscriptionStatus.TERMINATED;
            case SUSPENDED -> targetStatus == SubscriptionStatus.ACTIVE || targetStatus == SubscriptionStatus.TERMINATED;
            case TERMINATED -> false;
        };
        if (!valid) {
            throw new InvalidStateTransitionException(
                    "Cannot transition from " + current + " to " + targetStatus);
        }
    }

    private String allocateMsisdn() {
        return "5" + String.format("%09d", ThreadLocalRandom.current().nextInt(100000000, 999999999));
    }

    private Map<String, Object> buildActivatedEvent(Subscription subscription) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("orderId", subscription.getOrderId() != null ? subscription.getOrderId().toString() : null);
        event.put("subscriptionId", subscription.getId().toString());
        event.put("customerId", subscription.getCustomerId().toString());
        event.put("tariffCode", subscription.getTariffCode());
        event.put("msisdn", subscription.getMsisdn());
        event.put("occurredAt", LocalDateTime.now().toString());
        return event;
    }

    private Map<String, Object> buildActivationFailedEvent(UUID orderId, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("orderId", orderId.toString());
        event.put("reason", reason);
        event.put("occurredAt", LocalDateTime.now().toString());
        return event;
    }
}
