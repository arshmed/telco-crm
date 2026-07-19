package com.telcocrm.customerservice.repository;

import com.telcocrm.customerservice.entity.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, String> {
    List<DocumentTypeEntity> findByActiveTrueOrderBySortOrderAsc();
}
