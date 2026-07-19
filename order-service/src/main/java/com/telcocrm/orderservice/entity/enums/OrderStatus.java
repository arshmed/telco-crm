package com.telcocrm.orderservice.entity.enums;

public enum OrderStatus {
    DRAFT,              // Sipariş oluşturuldu ama henüz işleme alınmadı
    PENDING_PAYMENT,    // Ödeme bekleniyor
    PAID,               // Ödeme alındı, abonelik açılması bekleniyor
    FULFILLED,          // Abonelik açıldı, sipariş tamamlandı
    CANCELLED           // İptal edildi (kompansasyon tamamlandı)
}
