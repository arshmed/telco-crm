package com.telcocrm.paymentservice.mapper;

import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PaymentAttemptMapper.class})
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);
}
