package com.telcocrm.orderservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.orderservice.entity.SagaState;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    Optional<SagaState> findByOrderId(UUID orderId); // siparişin saga durumunu getirir

}   
