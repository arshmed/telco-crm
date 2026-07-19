package com.telcocrm.usageservice.event.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.usageservice.entity.enums.UsageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdrRecordedEventJsonTest {

    @Test
    void deserializesFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = "{\"eventId\":\"aaaa0009-0000-0000-0000-000000000009\",\"occurredAt\":\"2026-07-05T15:00:00\",\"subscriptionId\":\"33333333-3333-3333-3333-333333333333\",\"type\":\"DATA\",\"quantity\":100,\"cdrRef\":\"CDR-0009\"}";
        CdrRecordedEvent event = mapper.readValue(json, CdrRecordedEvent.class);
        assertEquals(UsageType.DATA, event.type());
        assertEquals(100, event.quantity());
    }
}
