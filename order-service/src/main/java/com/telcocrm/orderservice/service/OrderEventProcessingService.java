package com.telcocrm.orderservice.service;

import com.telcocrm.orderservice.event.consume.PaymentCompletedEvent;
import com.telcocrm.orderservice.event.consume.PaymentFailedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.orderservice.event.consume.SubscriptionActivationFailedEvent;

public interface OrderEventProcessingService {

    void processPaymentCompleted(PaymentCompletedEvent event);

    void processPaymentFailed(PaymentFailedEvent event);

    void processSubscriptionActivated(SubscriptionActivatedEvent event);

    void processSubscriptionActivationFailed(SubscriptionActivationFailedEvent event);
}
