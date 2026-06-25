package com.telcocrm.orderservice.entity;

import com.telcocrm.orderservice.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Hangi müşterinin siparişi — customer-service'den doğrulanacak
    // Burada Customer entity'si yok, sadece ID tutuyoruz (database-per-service pattern)
    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING) // DB'ye "DRAFT" gibi yazı olarak kaydeder, sayı değil
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2) // 12 basamaklı, 2 ondalık basamaklı decimal
    private BigDecimal totalAmount;

    // Para birimi ayrı tutuyoruz — ileride çoklu para birimi desteklenebilir
    @Column(nullable = false, length = 3)
    private String currency;

    // Ödeme tamamlandığında payment-service'den gelecek ID
    // Başlangıçta null, PaymentCompleted eventi gelince dolar
    @Column
    private UUID paymentId;

    // Abonelik açıldığında subscription-service'den gelecek ID
    // Başlangıçta null, SubscriptionActivated eventi gelince dolar
    @Column
    private UUID subscriptionId;

    // Saga ile hangi adımda iptal edildiğini anlamak için iptal sebebi
    @Column(length = 500)
    private String cancellationReason;

    // Order silinmez, sadece CANCELLED yapılır — ama yine de soft delete ekledik
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp // Entity ilk kaydedildiğinde otomatik set edilir
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Her güncellemede otomatik set edilir
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Bir siparişin birden fazla ürünü olabilir (tarife + addon gibi)
    // CascadeType.ALL → Order kaydedilince OrderItem'lar da kaydedilir
    // orphanRemoval → Order'dan çıkarılan item DB'den de silinir
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // OneToOne → Her siparişin bir Saga durumu var
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private SagaState sagaState;
}