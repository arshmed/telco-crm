package com.telcocrm.usageservice.client.dto;

import java.util.UUID;

public record TariffResponse(
        UUID id,
        String code,
        Integer minutesIncluded,
        Integer smsIncluded,
        Integer dataMbIncluded
) {}
