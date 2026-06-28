package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    private UUID id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private int retryCount;

    private Instant createdAt;

    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
}