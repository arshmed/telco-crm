package com.telcocrm.notificationservice.repository;

import com.telcocrm.notificationservice.entity.Notification;
import com.telcocrm.notificationservice.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
}
