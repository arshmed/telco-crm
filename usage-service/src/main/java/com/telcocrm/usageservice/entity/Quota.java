package com.telcocrm.usageservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotas", uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "period_start"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column(nullable = false)
    private String tariffCode;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Integer minutesIncluded;

    @Column(nullable = false)
    private Integer smsIncluded;

    @Column(nullable = false)
    private Integer dataMbIncluded;

    @Column(nullable = false)
    @Builder.Default
    private Integer minutesUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer smsUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer dataMbUsed = 0;

    @Column
    private LocalDateTime aggregatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
