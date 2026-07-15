package com.telcocrm.subscriptionservice.repository;

import com.telcocrm.subscriptionservice.entity.Subscription;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT FUNCTION('TO_CHAR', s.activatedAt, 'YYYY-MM') AS month, COUNT(s) " +
            "FROM Subscription s " +
            "WHERE s.activatedAt IS NOT NULL AND s.activatedAt >= :since " +
            "GROUP BY FUNCTION('TO_CHAR', s.activatedAt, 'YYYY-MM') " +
            "ORDER BY month")
    List<Object[]> countMonthlyActivations(@Param("since") LocalDateTime since);

    @Query("SELECT s.tariffCode, COUNT(s) FROM Subscription s " +
            "WHERE s.status = :status " +
            "GROUP BY s.tariffCode " +
            "ORDER BY COUNT(s) DESC")
    List<Object[]> countByTariffCode(@Param("status") SubscriptionStatus status);
}
