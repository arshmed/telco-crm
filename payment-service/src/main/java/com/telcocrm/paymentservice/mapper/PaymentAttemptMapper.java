package com.telcocrm.paymentservice.mapper;

import com.telcocrm.paymentservice.dto.response.PaymentAttemptResponse;
import com.telcocrm.paymentservice.entity.PaymentAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PaymentAttemptMapper {

    PaymentAttemptResponse toResponse(PaymentAttempt paymentAttempt);
}
