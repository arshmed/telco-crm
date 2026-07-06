package com.telcocrm.notificationservice.repository;

import com.telcocrm.notificationservice.entity.UserCommunicationPreference;
import com.telcocrm.notificationservice.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCommunicationPreferenceRepository extends JpaRepository<UserCommunicationPreference, UUID> {
    Optional<UserCommunicationPreference> findByUserIdAndChannel(UUID userId, NotificationChannel channel);
}
