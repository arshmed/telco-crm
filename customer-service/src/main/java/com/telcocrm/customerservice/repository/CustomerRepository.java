package com.telcocrm.customerservice.repository;

import com.telcocrm.customerservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByIdentityNumber(String identityNumber);
    boolean existsByIdentityNumber(String identityNumber);
}
