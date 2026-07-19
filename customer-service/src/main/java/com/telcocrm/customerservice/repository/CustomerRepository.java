package com.telcocrm.customerservice.repository;

import com.telcocrm.customerservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByIdentityNumber(String identityNumber);
    Optional<Customer> findByCustomerNo(String customerNo);
    boolean existsByIdentityNumber(String identityNumber);
    boolean existsByIdentityNumberHash(String identityNumberHash);

    @Query("SELECT MAX(c.customerNo) FROM Customer c")
    Optional<String> findMaxCustomerNo();
}
