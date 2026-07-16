package com.telcocrm.ticketservice.controller;

import com.telcocrm.ticketservice.dto.request.AddCommentRequest;
import com.telcocrm.ticketservice.dto.request.AssignTicketRequest;
import com.telcocrm.ticketservice.dto.request.CreateTicketRequest;
import com.telcocrm.ticketservice.dto.request.ResolveTicketRequest;
import com.telcocrm.ticketservice.dto.response.TicketCommentResponse;
import com.telcocrm.ticketservice.dto.response.TicketResponse;
import com.telcocrm.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketCommentResponse> addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        TicketCommentResponse response = ticketService.addComment(ticketId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, request));
    }

    @PostMapping("/{ticketId}/resolve")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody ResolveTicketRequest request) {
        return ResponseEntity.ok(ticketService.resolveTicket(ticketId, request));
    }
}
