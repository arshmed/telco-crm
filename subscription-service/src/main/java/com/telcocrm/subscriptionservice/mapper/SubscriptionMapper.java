package com.telcocrm.subscriptionservice.mapper;

import com.telcocrm.subscriptionservice.dto.response.SubscriptionResponse;
import com.telcocrm.subscriptionservice.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toResponse(Subscription subscription);

    void updateEntity(@MappingTarget Subscription subscription, SubscriptionResponse response);
}
