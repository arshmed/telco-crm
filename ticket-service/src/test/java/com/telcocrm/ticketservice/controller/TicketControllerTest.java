package com.telcocrm.ticketservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.ticketservice.dto.request.AddCommentRequest;
import com.telcocrm.ticketservice.dto.request.AssignTicketRequest;
import com.telcocrm.ticketservice.dto.request.CreateTicketRequest;
import com.telcocrm.ticketservice.dto.request.ResolveTicketRequest;
import com.telcocrm.ticketservice.dto.response.TicketCommentResponse;
import com.telcocrm.ticketservice.dto.response.TicketResponse;
import com.telcocrm.ticketservice.dto.response.TicketSummaryResponse;
import com.telcocrm.ticketservice.entity.enums.TicketCategory;
import com.telcocrm.ticketservice.entity.enums.TicketPriority;
import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import com.telcocrm.ticketservice.exception.GlobalExceptionHandler;
import com.telcocrm.ticketservice.exception.TicketNotFoundException;
import com.telcocrm.ticketservice.exception.TicketNotModifiableException;
import com.telcocrm.ticketservice.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    private final ObjectMapper objectMapper;
    private final UUID ticketId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    }

    @BeforeEach
    void setUp() {
        var controller = new TicketController(ticketService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private TicketResponse sampleResponse() {
        return new TicketResponse(ticketId, customerId, TicketCategory.COMPLAINT, TicketPriority.HIGH,
                TicketStatus.ASSIGNED, "Fazla ücret", "complaint-team",
                LocalDateTime.now().plusHours(8), null, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private TicketSummaryResponse sampleSummary() {
        return new TicketSummaryResponse(ticketId, customerId, "Ayşe Yılmaz",
                TicketCategory.COMPLAINT, TicketPriority.HIGH, TicketStatus.ASSIGNED,
                "Fazla ücret", "complaint-team",
                LocalDateTime.now().plusHours(8), LocalDateTime.now());
    }

    @Test
    void shouldListTicketsAndReturn200() throws Exception {
        when(ticketService.listTickets(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$.content[0].customerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.content[0].comments").doesNotExist());
    }

    @Test
    void shouldPassStatusFilterToService() throws Exception {
        when(ticketService.listTickets(eq(TicketStatus.ASSIGNED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/tickets").param("status", "ASSIGNED"))
                .andExpect(status().isOk());

        verify(ticketService).listTickets(eq(TicketStatus.ASSIGNED), any(Pageable.class));
    }

    @Test
    void shouldPassPaginationParamsToService() throws Exception {
        when(ticketService.listTickets(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketService).listTickets(eq(null), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void shouldCreateTicketAndReturn201WithLocation() throws Exception {
        var request = new CreateTicketRequest(customerId, TicketCategory.COMPLAINT, TicketPriority.HIGH,
                "Fazla ücret");
        when(ticketService.createTicket(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + ticketId))
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.assignedTeam").value("complaint-team"));
    }

    @Test
    void shouldReturn400WhenDescriptionIsBlank() throws Exception {
        var request = new CreateTicketRequest(customerId, TicketCategory.COMPLAINT, TicketPriority.HIGH, "");

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void shouldReturn400WhenCustomerIdIsMissing() throws Exception {
        var request = new CreateTicketRequest(null, TicketCategory.COMPLAINT, TicketPriority.HIGH, "Açıklama");

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerId").exists());
    }

    @Test
    void shouldReturnTicketById() throws Exception {
        when(ticketService.getTicketById(ticketId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()));
    }

    @Test
    void shouldReturn404WhenTicketMissing() throws Exception {
        when(ticketService.getTicketById(ticketId)).thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.errorCode").value("TICKET_NOT_FOUND"));
    }

    @Test
    void shouldAddCommentUsingAuthenticatedUserAsAuthor() throws Exception {
        var request = new AddCommentRequest("bakıyorum");
        when(ticketService.addComment(eq(ticketId), any(), eq("agent-7")))
                .thenReturn(new TicketCommentResponse(UUID.randomUUID(), "agent-7", "bakıyorum", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticketId)
                        .principal(new UsernamePasswordAuthenticationToken("agent-7", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value("agent-7"));

        verify(ticketService).addComment(eq(ticketId), any(), eq("agent-7"));
    }

    @Test
    void shouldAssignTicket() throws Exception {
        when(ticketService.assignTicket(eq(ticketId), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tickets/{id}/assign", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTicketRequest("fault-team"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenAssignedTeamIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/assign", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTicketRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.assignedTeam").exists());
    }

    @Test
    void shouldReturn409WhenResolvingResolvedTicket() throws Exception {
        when(ticketService.resolveTicket(eq(ticketId), any()))
                .thenThrow(new TicketNotModifiableException(ticketId, TicketStatus.RESOLVED, "resolved"));

        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveTicketRequest("tekrar"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("TICKET_NOT_MODIFIABLE"));
    }

    @Test
    void shouldResolveTicket() throws Exception {
        when(ticketService.resolveTicket(eq(ticketId), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveTicketRequest("Fatura düzeltildi"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenResolutionIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveTicketRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.resolution").exists());
    }
}
