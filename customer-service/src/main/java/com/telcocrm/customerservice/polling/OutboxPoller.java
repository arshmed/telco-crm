package com.telcocrm.customerservice.polling;

import com.telcocrm.customerservice.entity.OutboxEvent;
import com.telcocrm.customerservice.enums.OutboxStatus;
import com.telcocrm.customerservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxRepository outboxRepository;
    private final StreamBridge streamBridge;

    @Scheduled(fixedDelay = 20_000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findPublishable(BATCH_SIZE, MAX_RETRY_COUNT);
        for (OutboxEvent event : events) {
            try {
                Message<byte[]> message = MessageBuilder
                    .withPayload(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                    .build();
                streamBridge.send(event.getEventType() + "-out-0", message);
                event.setStatus(OutboxStatus.SENT);
                event.setErrorMessage(null);
                log.debug("Published outbox event: {}", event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getEventType(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
            event.setProcessedAt(Instant.now());
            outboxRepository.save(event);
        }
    }
}
