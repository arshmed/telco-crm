package com.telcocrm.subscriptionservice.controller;

import com.telcocrm.subscriptionservice.dto.request.CreateSubscriptionRequest;
import com.telcocrm.subscriptionservice.dto.response.SubscriptionResponse;
import com.telcocrm.subscriptionservice.enums.SubscriptionStatus;
import com.telcocrm.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.getSubscription(id));
    }

    @GetMapping
    public ResponseEntity<Page<SubscriptionResponse>> getSubscriptions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getSubscriptions(pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<SubscriptionResponse>> getByCustomer(
            @PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByCustomer(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<SubscriptionResponse>> getByStatus(
            @PathVariable SubscriptionStatus status, Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByStatus(status, pageable));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<SubscriptionResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.activateSubscription(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<SubscriptionResponse> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.suspendSubscription(id));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.reactivateSubscription(id));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<SubscriptionResponse> terminate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.terminateSubscription(id));
    }
}
