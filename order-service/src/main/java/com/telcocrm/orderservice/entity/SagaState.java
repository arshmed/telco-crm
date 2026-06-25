package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.SagaStep;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStep currentStep;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}