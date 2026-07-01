package com.telcocrm.productcatalogservice.messaging;

import com.telcocrm.productcatalogservice.entity.OutboxEvent;
import com.telcocrm.productcatalogservice.entity.enums.OutboxStatus;
import com.telcocrm.productcatalogservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    private final OutboxRepository outboxRepository;
    private final StreamBridge streamBridge;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        pending.forEach(this::dispatch);
    }

    private void dispatch(OutboxEvent event) {
        try {
            streamBridge.send(destinationFor(event.getType()), toMessage(event));
            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastRetry(Instant.now());
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
            }
        }
        outboxRepository.save(event);
    }

    private Message<byte[]> toMessage(OutboxEvent event) {
        return MessageBuilder
                .withPayload(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                .setHeader(KafkaHeaders.KEY, event.getAggregateId().getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private String destinationFor(String type) {
        return type.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
