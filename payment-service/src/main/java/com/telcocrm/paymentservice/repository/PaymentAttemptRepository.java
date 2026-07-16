package com.telcocrm.paymentservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.telcocrm.paymentservice.entity.PaymentAttempt;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
}
