package com.telcocrm.usageservice.kafka.consumer;

import com.telcocrm.usageservice.event.consume.CdrRecordedEvent;
import com.telcocrm.usageservice.event.consume.SubscriptionActivatedEvent;
import com.telcocrm.usageservice.service.UsageEventProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
public class UsageEventConsumer {

    private final UsageEventProcessingService usageEventProcessingService;

    @Bean
    public Consumer<SubscriptionActivatedEvent> subscriptionActivatedEvent() {
        return usageEventProcessingService::processSubscriptionActivated;
    }

    @Bean
    public Consumer<CdrRecordedEvent> cdrRecordedEvent() {
        return usageEventProcessingService::processCdrRecorded;
    }
}
