package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.event.consume.OrderCreatedEvent;
import com.telcocrm.paymentservice.event.consume.SubscriptionActivationFailedEvent;

public interface PaymentEventProcessingService {

    void processOrderCreated(OrderCreatedEvent event);

    void processSubscriptionActivationFailed(SubscriptionActivationFailedEvent event);
}
