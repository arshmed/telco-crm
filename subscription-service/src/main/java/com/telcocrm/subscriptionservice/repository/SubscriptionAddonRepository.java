package com.telcocrm.subscriptionservice.repository;

import com.telcocrm.subscriptionservice.entity.SubscriptionAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionAddonRepository extends JpaRepository<SubscriptionAddon, UUID> {

    List<SubscriptionAddon> findBySubscriptionId(UUID subscriptionId);
}
