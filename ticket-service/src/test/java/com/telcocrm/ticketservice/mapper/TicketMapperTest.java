package com.telcocrm.ticketservice.mapper;

import com.telcocrm.ticketservice.dto.response.TicketResponse;
import com.telcocrm.ticketservice.dto.response.TicketSummaryResponse;
import com.telcocrm.ticketservice.entity.Ticket;
import com.telcocrm.ticketservice.entity.TicketComment;
import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperTest {

    private final TicketCommentMapper commentMapper = new TicketCommentMapperImpl();
    private final TicketMapper mapper = ticketMapperWith(commentMapper);

    private static TicketMapper ticketMapperWith(TicketCommentMapper commentMapper) {
        var impl = new TicketMapperImpl();
        ReflectionTestUtils.setField(impl, "ticketCommentMapper", commentMapper);
        return impl;
    }

    @Test
    void shouldMapNullTicketToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void shouldMapAllTicketFields() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.builder()
                .id(id)
                .customerId(customerId)
                .category(TicketCategory.FAULT)
                .priority(TicketPriority.URGENT)
                .status(TicketStatus.RESOLVED)
                .description("İki gündür sinyal yok")
                .assignedTeam("fault-team")
                .slaDueAt(now.plusHours(4))
                .resolution("baz istasyonu onarıldı")
                .resolvedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        TicketResponse response = mapper.toResponse(ticket);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.category()).isEqualTo(TicketCategory.FAULT);
        assertThat(response.priority()).isEqualTo(TicketPriority.URGENT);
        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.description()).isEqualTo("İki gündür sinyal yok");
        assertThat(response.assignedTeam()).isEqualTo("fault-team");
        assertThat(response.slaDueAt()).isEqualTo(now.plusHours(4));
        assertThat(response.resolution()).isEqualTo("baz istasyonu onarıldı");
        assertThat(response.resolvedAt()).isEqualTo(now);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void shouldMapNestedComments() {
        LocalDateTime now = LocalDateTime.now();
        UUID commentId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .category(TicketCategory.COMPLAINT)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.ASSIGNED)
                .description("açıklama")
                .assignedTeam("complaint-team")
                .slaDueAt(now.plusHours(72))
                .comments(List.of(TicketComment.builder()
                        .id(commentId)
                        .authorId("agent-1")
                        .body("bakıyorum")
                        .createdAt(now)
                        .build()))
                .build();

        TicketResponse response = mapper.toResponse(ticket);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).id()).isEqualTo(commentId);
        assertThat(response.comments().get(0).authorId()).isEqualTo("agent-1");
        assertThat(response.comments().get(0).body()).isEqualTo("bakıyorum");
    }

    @Test
    void shouldMapTicketToSummaryResponseWithoutComments() {
        UUID id = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.builder()
                .id(id)
                .customerId(customerId)
                .customerName("Ayşe Yılmaz")
                .category(TicketCategory.FAULT)
                .priority(TicketPriority.URGENT)
                .status(TicketStatus.ASSIGNED)
                .description("İki gündür sinyal yok")
                .assignedTeam("fault-team")
                .slaDueAt(now.plusHours(4))
                .createdAt(now)
                .updatedAt(now)
                .build();

        TicketSummaryResponse response = mapper.toSummaryResponse(ticket);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.customerName()).isEqualTo("Ayşe Yılmaz");
        assertThat(response.category()).isEqualTo(TicketCategory.FAULT);
        assertThat(response.priority()).isEqualTo(TicketPriority.URGENT);
        assertThat(response.status()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(response.description()).isEqualTo("İki gündür sinyal yok");
        assertThat(response.assignedTeam()).isEqualTo("fault-team");
        assertThat(response.slaDueAt()).isEqualTo(now.plusHours(4));
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void shouldMapCommentDirectly() {
        UUID commentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        var response = commentMapper.toResponse(TicketComment.builder()
                .id(commentId)
                .authorId("agent-2")
                .body("not")
                .createdAt(now)
                .build());

        assertThat(response.id()).isEqualTo(commentId);
        assertThat(response.authorId()).isEqualTo("agent-2");
        assertThat(response.body()).isEqualTo("not");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void shouldMapNullCommentToNull() {
        assertThat(commentMapper.toResponse(null)).isNull();
    }
}
