package com.telcocrm.orderservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.orderservice.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

}
