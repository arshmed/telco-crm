package com.telcocrm.ticketservice.rules;

import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketSlaRules {

    // ponytail: SLA süreleri iş birimi tarafından teyit edilmedi, placeholder.
    // Gerçek değerler gelince burayı güncelle; ekip başına farklılaşırsa config'e taşı.
    private static final Map<TicketPriority, Integer> SLA_HOURS_BY_PRIORITY = Map.of(
            TicketPriority.URGENT, 4,
            TicketPriority.HIGH, 8,
            TicketPriority.MEDIUM, 24,
            TicketPriority.LOW, 72
    );

    private final Clock clock;

    // ponytail: ekip adı kategoriden türetiliyor; gerçek ekip adları netleşince map'e çevir.
    public String resolveTeam(TicketCategory category) {
        return category.name().toLowerCase(Locale.ROOT) + "-team";
    }

    public int slaHours(TicketPriority priority) {
        return SLA_HOURS_BY_PRIORITY.get(priority);
    }

    public LocalDateTime calculateSlaDueAt(TicketPriority priority) {
        return LocalDateTime.now(clock).plusHours(slaHours(priority));
    }
}
