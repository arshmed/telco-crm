package com.telcocrm.orderservice.entity;

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

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Instant createdAt;
}
