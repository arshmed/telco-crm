package com.telcocrm.usageservice.repository;

import com.telcocrm.usageservice.entity.Quota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotaRepository extends JpaRepository<Quota, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quota q where q.subscriptionId = :subscriptionId and :date between q.periodStart and q.periodEnd")
    Optional<Quota> findActiveForUpdate(@Param("subscriptionId") UUID subscriptionId, @Param("date") LocalDate date);

    @Query("select q from Quota q where q.subscriptionId = :subscriptionId and :date between q.periodStart and q.periodEnd")
    Optional<Quota> findActive(@Param("subscriptionId") UUID subscriptionId, @Param("date") LocalDate date);

    List<Quota> findByPeriodEndBeforeAndAggregatedAtIsNull(LocalDate date);

    Optional<Quota> findFirstBySubscriptionIdOrderByPeriodStartDesc(UUID subscriptionId);
}
