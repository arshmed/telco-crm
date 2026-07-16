package com.telcocrm.billingservice.handler;

import com.telcocrm.billingservice.entity.ProcessedEvent;
import com.telcocrm.billingservice.repository.ProcessedEventRepository;
import com.telcocrm.billingservice.service.BillRunService;
import com.telcocrm.billingservice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final BillRunService billRunService;
    private final InvoiceService invoiceService;

    @Bean
    public Consumer<java.util.Map<String, Object>> subscriptionActivatedEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing SubscriptionActivatedEvent: {}", eventId);

            UUID customerId = UUID.fromString((String) event.get("customerId"));
            UUID subscriptionId = UUID.fromString((String) event.get("subscriptionId"));
            String tariffCode = (String) event.get("tariffCode");

            billRunService.createBillCycle(customerId, subscriptionId, tariffCode, 1);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());

            log.info("Bill cycle created for subscription {}", subscriptionId);
        };
    }

    @Bean
    public Consumer<java.util.Map<String, Object>> usageAggregatedEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing UsageAggregatedEvent: {}", eventId);

            UUID customerId = UUID.fromString((String) event.get("customerId"));
            UUID subscriptionId = UUID.fromString((String) event.get("subscriptionId"));

            Object overageMinutes = event.get("overageMinutes");
            Object overageSms = event.get("overageSms");
            Object overageDataMb = event.get("overageDataMb");

            log.info("Usage aggregation for subscription {}: minutes={}, sms={}, dataMb={}",
                    subscriptionId, overageMinutes, overageSms, overageDataMb);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());
        };
    }

    @Bean
    public Consumer<java.util.Map<String, Object>> paymentCompletedEvent() {
        return event -> {
            UUID eventId = UUID.fromString((String) event.get("eventId"));
            if (processedEventRepository.existsById(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            log.info("Processing PaymentCompletedEvent: {}", eventId);

            Object invoiceIdObj = event.get("invoiceId");
            if (invoiceIdObj != null) {
                UUID invoiceId = UUID.fromString(invoiceIdObj.toString());
                invoiceService.markInvoicePaid(invoiceId);
            }

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .processedAt(Instant.now())
                    .build());

            log.info("Payment completed event processed: {}", eventId);
        };
    }
}
