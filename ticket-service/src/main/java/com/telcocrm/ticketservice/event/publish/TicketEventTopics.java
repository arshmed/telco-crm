package com.telcocrm.ticketservice.event.publish;

public final class TicketEventTopics {

    public static final String AGGREGATE_TYPE = "TICKET";

    public static final String TICKET_OPENED = "ticket-opened-topic";
    public static final String TICKET_RESOLVED = "ticket-resolved-topic";
    public static final String SLA_BREACHED = "sla-breached-topic";

    private TicketEventTopics() {
    }
}
