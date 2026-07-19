package com.telcocrm.billingservice.repository;

import com.telcocrm.billingservice.entity.BillCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillCycleRepository extends JpaRepository<BillCycle, UUID> {
    List<BillCycle> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
    Optional<BillCycle> findBySubscriptionId(UUID subscriptionId);
    List<BillCycle> findByCustomerId(UUID customerId);
}
