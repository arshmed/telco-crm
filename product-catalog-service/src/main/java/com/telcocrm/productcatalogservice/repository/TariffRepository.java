package com.telcocrm.productcatalogservice.repository;

import com.telcocrm.productcatalogservice.entity.Tariff;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {

    Optional<Tariff> findByCodeAndDeletedFalseAndCurrentTrue(String code);

    Optional<Tariff> findByCodeAndVersionAndDeletedFalse(String code, Integer version);

    List<Tariff> findByCodeAndDeletedFalseOrderByVersionDesc(String code);

    Page<Tariff> findByDeletedFalseAndCurrentTrue(Pageable pageable);

    Page<Tariff> findByStatusAndDeletedFalseAndCurrentTrue(TariffStatus status, Pageable pageable);

    boolean existsByCode(String code);
}
