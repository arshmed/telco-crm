package com.telcocrm.customerservice.repository;

import com.telcocrm.customerservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
        SELECT * FROM outbox
        WHERE status = 'PENDING' AND retry_count < :maxRetryCount
        ORDER BY created_at
        LIMIT :limit
        """, nativeQuery = true)
    List<OutboxEvent> findPublishable(@Param("limit") int limit, @Param("maxRetryCount") int maxRetryCount);
}
