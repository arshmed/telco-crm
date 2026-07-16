package com.telcocrm.orderservice.event.consume;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedEvent(
    UUID eventId,
    LocalDateTime occurredAt,
    UUID orderId,
    UUID paymentId,
    BigDecimal refundedAmount
) {}
