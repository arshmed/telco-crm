package com.telcocrm.orderservice.entity.enums;

public enum SagaStep {
    ORDER_CREATED,           // Sipariş oluşturuldu, ödeme bekleniyor
    AWAITING_PAYMENT,        // PaymentCompleted eventi bekleniyor
    AWAITING_SUBSCRIPTION,   // SubscriptionActivated eventi bekleniyor
    COMPLETED,               // Her şey tamam
    COMPENSATING,            // Bir şeyler ters gitti, geri alınıyor
    FAILED                   // Kompansasyon da tamamlandı, sipariş iptal
}
