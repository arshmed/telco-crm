package com.telcocrm.ticketservice.mapper;

import com.telcocrm.ticketservice.dto.response.TicketResponse;
import com.telcocrm.ticketservice.dto.response.TicketSummaryResponse;
import com.telcocrm.ticketservice.entity.Ticket;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TicketCommentMapper.class})
public interface TicketMapper {

    TicketResponse toResponse(Ticket ticket);

    TicketSummaryResponse toSummaryResponse(Ticket ticket);
}
