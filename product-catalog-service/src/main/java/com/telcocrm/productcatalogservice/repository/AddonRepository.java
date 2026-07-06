package com.telcocrm.productcatalogservice.repository;

import com.telcocrm.productcatalogservice.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddonRepository extends JpaRepository<Addon, UUID> {

    Optional<Addon> findByCodeAndDeletedFalse(String code);

    List<Addon> findByDeletedFalse();

    boolean existsByCode(String code);
}
