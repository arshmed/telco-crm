package com.telcocrm.paymentservice.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.paymentservice.entity.PaymentAuditLog;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, UUID> {

    List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

}
