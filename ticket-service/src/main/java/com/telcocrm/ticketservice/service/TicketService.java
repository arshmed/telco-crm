package com.telcocrm.ticketservice.service;

import com.telcocrm.ticketservice.dto.request.AddCommentRequest;
import com.telcocrm.ticketservice.dto.request.AssignTicketRequest;
import com.telcocrm.ticketservice.dto.request.CreateTicketRequest;
import com.telcocrm.ticketservice.dto.request.ResolveTicketRequest;
import com.telcocrm.ticketservice.dto.response.TicketCommentResponse;
import com.telcocrm.ticketservice.dto.response.TicketResponse;

import java.util.UUID;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse getTicketById(UUID ticketId);

    TicketCommentResponse addComment(UUID ticketId, AddCommentRequest request, String authorId);

    TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request);

    TicketResponse resolveTicket(UUID ticketId, ResolveTicketRequest request);
}
