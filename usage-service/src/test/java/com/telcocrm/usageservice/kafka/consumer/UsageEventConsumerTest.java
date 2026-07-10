package com.telcocrm.usageservice.kafka.consumer;

import com.telcocrm.usageservice.event.consume.CdrRecordedEvent;
import com.telcocrm.usageservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.usageservice.service.UsageEventProcessingService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UsageEventConsumerTest {

    private final UsageEventProcessingService processingService = mock(UsageEventProcessingService.class);
    private final UsageEventConsumer consumer = new UsageEventConsumer(processingService);

    @Test
    void subscriptionActivatedBeanDelegatesToProcessingService() {
        SubscriptionActivatedEvent event = mock(SubscriptionActivatedEvent.class);

        consumer.subscriptionActivatedEvent().accept(event);

        verify(processingService).processSubscriptionActivated(event);
    }

    @Test
    void cdrRecordedBeanDelegatesToProcessingService() {
        CdrRecordedEvent event = mock(CdrRecordedEvent.class);

        consumer.cdrRecordedEvent().accept(event);

        verify(processingService).processCdrRecorded(event);
    }
}
