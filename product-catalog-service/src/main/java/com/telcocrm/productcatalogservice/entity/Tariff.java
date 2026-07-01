package com.telcocrm.productcatalogservice.entity;

import com.telcocrm.productcatalogservice.entity.enums.TariffSegment;
import com.telcocrm.productcatalogservice.entity.enums.TariffStatus;
import com.telcocrm.productcatalogservice.entity.enums.TariffType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tariffs", uniqueConstraints = @UniqueConstraint(columnNames = {"code", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean current = true;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TariffSegment segment = TariffSegment.ALL;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column
    private Integer minutesIncluded;

    @Column
    private Integer smsIncluded;

    @Column
    private Integer dataMbIncluded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TariffStatus status = TariffStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column
    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tariff_addon",
            joinColumns = @JoinColumn(name = "tariff_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id")
    )
    @Builder.Default
    private Set<Addon> addons = new HashSet<>();
}
