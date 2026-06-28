package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderResponse toResponse(Order order);
}
