package com.telcocrm.ticketservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TicketNotFoundException extends BaseException {

    public TicketNotFoundException(UUID ticketId) {
        super(
            "Ticket not found with id: " + ticketId,
            HttpStatus.NOT_FOUND,
            "TICKET_NOT_FOUND"
        );
    }
}
