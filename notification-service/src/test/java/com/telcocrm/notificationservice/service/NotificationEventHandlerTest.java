package com.telcocrm.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private NotificationEventHandler eventHandler;

    @Test
    void shouldHandleCustomerRegisteredEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString(),
                "firstName", "John", "lastName", "Doe");
        eventHandler.customerRegisteredEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("CUSTOMER_REGISTERED"), any(), any());
    }

    @Test
    void shouldHandleCustomerKycApprovedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.customerKycApprovedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("CUSTOMER_KYC_APPROVED"), any(), any());
    }

    @Test
    void shouldHandleCustomerKycRejectedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.customerKycRejectedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("CUSTOMER_KYC_REJECTED"), any(), any());
    }

    @Test
    void shouldHandleCustomerUpdatedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.customerUpdatedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("CUSTOMER_UPDATED"), any(), any());
    }

    @Test
    void shouldHandleTicketOpenedEvent() {
        UUID customerId = UUID.randomUUID();
        var event = Map.<String, Object>of("customerId", customerId.toString(),
                "ticketId", UUID.randomUUID().toString(), "email", "john@example.com");
        eventHandler.ticketOpenedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent("TICKET_OPENED", customerId, event);
    }

    @Test
    void shouldHandleTicketResolvedEvent() {
        UUID customerId = UUID.randomUUID();
        var event = Map.<String, Object>of("customerId", customerId.toString(),
                "ticketId", UUID.randomUUID().toString(), "resolution", "Fixed");
        eventHandler.ticketResolvedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent("TICKET_RESOLVED", customerId, event);
    }

    @Test
    void shouldHandleOrderCreatedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.orderCreatedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("ORDER_CREATED"), any(), any());
    }

    @Test
    void shouldHandleOrderConfirmedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.orderConfirmedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("ORDER_CONFIRMED"), any(), any());
    }

    @Test
    void shouldHandleOrderCancelledEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.orderCancelledEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("ORDER_CANCELLED"), any(), any());
    }

    @Test
    void shouldHandleQuotaThresholdReachedEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.quotaThresholdReachedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("QUOTA_THRESHOLD_REACHED"), any(), any());
    }

    @Test
    void shouldHandleQuotaExceededEvent() {
        var event = Map.<String, Object>of("customerId", UUID.randomUUID().toString());
        eventHandler.quotaExceededEvent().accept(event);
        verify(dispatcher).dispatchFromEvent(eq("QUOTA_EXCEEDED"), any(), any());
    }

    @Test
    void shouldHandleEventWithUuidCustomerId() {
        UUID customerId = UUID.randomUUID();
        var event = Map.<String, Object>of("customerId", customerId);
        eventHandler.customerUpdatedEvent().accept(event);
        verify(dispatcher).dispatchFromEvent("CUSTOMER_UPDATED", customerId, event);
    }
}
