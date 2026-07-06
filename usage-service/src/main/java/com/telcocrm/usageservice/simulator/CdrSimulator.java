package com.telcocrm.usageservice.simulator;

import com.telcocrm.usageservice.entity.Quota;
import com.telcocrm.usageservice.entity.enums.UsageType;
import com.telcocrm.usageservice.event.consume.CdrRecordedEvent;
import com.telcocrm.usageservice.repository.QuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "usage.cdr-simulator.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CdrSimulator {

    private static final Random RANDOM = new Random();

    private final QuotaRepository quotaRepository;
    private final StreamBridge streamBridge;

    @Scheduled(fixedDelayString = "${usage.cdr-simulator.interval-ms:10000}")
    public void emitRandomCdr() {
        List<Quota> quotas = quotaRepository.findAll();
        if (quotas.isEmpty()) {
            log.info("CDR simulator: no quotas found, skipping tick");
            return;
        }

        Quota quota = quotas.get(RANDOM.nextInt(quotas.size()));
        UsageType type = UsageType.values()[RANDOM.nextInt(UsageType.values().length)];
        int quantity = switch (type) {
            case VOICE -> 1 + RANDOM.nextInt(30);
            case SMS -> 1 + RANDOM.nextInt(5);
            case DATA -> 10 + RANDOM.nextInt(500);
        };

        CdrRecordedEvent event = new CdrRecordedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                quota.getSubscriptionId(),
                type,
                quantity,
                "SIM-" + UUID.randomUUID()
        );

        streamBridge.send("cdr-recorded-topic", event);
        log.info("Simulated CDR sent: {} {} for subscriptionId: {}", type, quantity, quota.getSubscriptionId());
    }
}
