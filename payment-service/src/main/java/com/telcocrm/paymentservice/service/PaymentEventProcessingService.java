package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;

public interface PaymentEventProcessingService {

    void processOrderCreated(OrderCreatedEvent event);
}
