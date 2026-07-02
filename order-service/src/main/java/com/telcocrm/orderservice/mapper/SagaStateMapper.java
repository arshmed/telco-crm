package com.telcocrm.orderservice.mapper;

import com.telcocrm.orderservice.dto.response.SagaStateResponse;
import com.telcocrm.orderservice.entity.SagaState;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SagaStateMapper {

    SagaStateResponse toResponse(SagaState sagaState);
}
