package com.telcocrm.billingservice.repository;

import com.telcocrm.billingservice.entity.Invoice;
import com.telcocrm.billingservice.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<Invoice> findAllByOrderByCreatedAtDesc();
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByStatus(InvoiceStatus status);
    boolean existsBySubscriptionIdAndPeriodStartAndPeriodEnd(UUID subscriptionId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);
}
