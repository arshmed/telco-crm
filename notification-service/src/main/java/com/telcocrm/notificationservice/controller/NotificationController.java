package com.telcocrm.notificationservice.controller;

import com.telcocrm.notificationservice.dto.NotificationRequest;
import com.telcocrm.notificationservice.dto.NotificationResponse;
import com.telcocrm.notificationservice.exception.ResourceNotFoundException;
import com.telcocrm.notificationservice.security.CustomerAccessGuard;
import com.telcocrm.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CustomerAccessGuard customerAccessGuard;

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<Page<NotificationResponse>> getUserNotificationHistory(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        customerAccessGuard.assertOwnResource(userId, () -> new ResourceNotFoundException("Notification", "userId", userId));
        return ResponseEntity.ok(notificationService.getUserNotificationHistory(userId, pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications() {
        return ResponseEntity.ok(notificationService.getRecentNotifications());
    }
}
