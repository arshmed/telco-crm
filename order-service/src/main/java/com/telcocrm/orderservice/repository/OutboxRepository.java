package com.telcocrm.orderservice.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.telcocrm.orderservice.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value="""
        SELECT * FROM outbox
        WHERE status = 'PENDING' and retry_count < :maxRetryCount
        ORDER BY created_at
        LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEvent> findPublishable(@Param("limit") int limit, @Param("maxRetryCount") int maxRetryCount);

}
