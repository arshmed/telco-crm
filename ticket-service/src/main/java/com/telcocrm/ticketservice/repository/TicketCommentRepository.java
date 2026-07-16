package com.telcocrm.ticketservice.repository;

import com.telcocrm.ticketservice.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
}
