package com.telcocrm.orderservice.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.orderservice.entity.OrderAuditLog;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, UUID> {

    List<OrderAuditLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

}
