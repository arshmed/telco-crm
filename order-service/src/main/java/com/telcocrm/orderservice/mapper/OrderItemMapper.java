package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.dto.response.OrderItemResponse;
import com.telcocrm.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {

    OrderItemResponse toResponse(OrderItem orderItem);
}
