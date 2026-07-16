package com.telcocrm.ticketservice.exception;

import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TicketNotModifiableException extends BaseException {

    public TicketNotModifiableException(UUID ticketId, TicketStatus status, String operation) {
        super(
            "Ticket " + ticketId + " cannot be " + operation + " while in status: " + status,
            HttpStatus.CONFLICT,
            "TICKET_NOT_MODIFIABLE"
        );
    }
}
