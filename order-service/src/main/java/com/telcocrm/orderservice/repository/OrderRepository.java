package com.telcocrm.orderservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.orderservice.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndDeletedFalse(UUID id); // sipariş getirirken silinmiş siparişleri getirmez

    Page<Order> findByCustomerIdAndDeletedFalse(UUID customerId, Pageable pageable);

    Page<Order> findByDeletedFalse(Pageable pageable);
}
