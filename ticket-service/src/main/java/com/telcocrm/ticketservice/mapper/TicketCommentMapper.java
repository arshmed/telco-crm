package com.telcocrm.ticketservice.mapper;

import com.telcocrm.ticketservice.dto.response.TicketCommentResponse;
import com.telcocrm.ticketservice.entity.TicketComment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketCommentMapper {

    TicketCommentResponse toResponse(TicketComment comment);
}
