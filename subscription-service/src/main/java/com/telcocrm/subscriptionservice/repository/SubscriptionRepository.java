package com.telcocrm.subscriptionservice.repository;

import com.telcocrm.subscriptionservice.entity.Subscription;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    List<Subscription> findByCustomerIdAndStatusIn(UUID customerId, List<SubscriptionStatus> statuses);

    Optional<Subscription> findByMsisdn(String msisdn);

    Optional<Subscription> findByOrderId(UUID orderId);

    Optional<Subscription> findByCustomerIdAndStatus(UUID customerId, SubscriptionStatus status);

    boolean existsByCustomerIdAndStatus(UUID customerId, SubscriptionStatus status);
}
