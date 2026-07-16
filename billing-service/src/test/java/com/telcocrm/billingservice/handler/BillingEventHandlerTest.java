package com.telcocrm.billingservice.handler;

import com.telcocrm.billingservice.entity.ProcessedEvent;
import com.telcocrm.billingservice.repository.ProcessedEventRepository;
import com.telcocrm.billingservice.service.BillRunService;
import com.telcocrm.billingservice.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingEventHandlerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private BillRunService billRunService;
    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private BillingEventHandler billingEventHandler;

    private Consumer<Map<String, Object>> subscriptionHandler;
    private Consumer<Map<String, Object>> usageHandler;
    private Consumer<Map<String, Object>> paymentHandler;

    @BeforeEach
    void setUp() {
        subscriptionHandler = billingEventHandler.subscriptionActivatedEvent();
        usageHandler = billingEventHandler.usageAggregatedEvent();
        paymentHandler = billingEventHandler.paymentCompletedEvent();
    }

    // --- subscriptionActivatedEvent ---

    @Test
    void subscriptionActivatedEvent_createsBillCycle() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "customerId", customerId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "tariffCode", "TARIFF-001"
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        subscriptionHandler.accept(event);

        verify(billRunService).createBillCycle(customerId, subscriptionId, "TARIFF-001", 1);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void subscriptionActivatedEvent_duplicateEvent_skips() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "tariffCode", "TARIFF-001"
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        subscriptionHandler.accept(event);

        verify(billRunService, never()).createBillCycle(any(), any(), anyString(), anyInt());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void subscriptionActivatedEvent_savesProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "tariffCode", "TARIFF-001"
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        subscriptionHandler.accept(event);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }

    // --- usageAggregatedEvent ---

    @Test
    void usageAggregatedEvent_logsAndPersists() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "overageMinutes", 10,
                "overageSms", 0,
                "overageDataMb", 500
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        usageHandler.accept(event);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void usageAggregatedEvent_duplicate_skips() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        usageHandler.accept(event);

        verify(processedEventRepository, never()).save(any());
    }

    // --- paymentCompletedEvent ---

    @Test
    void paymentCompletedEvent_marksInvoicePaid() {
        UUID eventId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "invoiceId", invoiceId.toString()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        paymentHandler.accept(event);

        verify(invoiceService).markInvoicePaid(invoiceId);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentCompletedEvent_noInvoiceId_skipsMarkPaid() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        paymentHandler.accept(event);

        verify(invoiceService, never()).markInvoicePaid(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void paymentCompletedEvent_duplicate_skips() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventId", eventId.toString(),
                "invoiceId", UUID.randomUUID().toString()
        );
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        paymentHandler.accept(event);

        verify(invoiceService, never()).markInvoicePaid(any());
        verify(processedEventRepository, never()).save(any());
    }
}
