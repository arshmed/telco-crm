package com.telcocrm.notificationservice.repository;

import com.telcocrm.notificationservice.entity.NotificationTemplate;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByCodeAndChannel(String code, NotificationChannel channel);
    List<NotificationTemplate> findByCode(String code);
}
