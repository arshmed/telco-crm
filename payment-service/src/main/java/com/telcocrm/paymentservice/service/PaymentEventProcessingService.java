package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.event.consume.SubscriptionActivationFailedEvent;

public interface PaymentEventProcessingService {

    void processSubscriptionActivationFailed(SubscriptionActivationFailedEvent event);
}
