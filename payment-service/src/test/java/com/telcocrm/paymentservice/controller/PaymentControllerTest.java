package com.telcocrm.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.exception.DuplicateRequestException;
import com.telcocrm.paymentservice.exception.GlobalExceptionHandler;
import com.telcocrm.paymentservice.exception.PaymentNotFoundException;
import com.telcocrm.paymentservice.exception.PaymentRefundException;
import com.telcocrm.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPaymentById_shouldReturn200() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("99.99"), "TRY",
                PaymentMethod.CREDIT_CARD, PaymentStatus.COMPLETED,
                "MOCK-REF-123", null, Instant.now(),
                List.of(), Instant.now(), Instant.now()
        );

        when(paymentService.getPaymentById(paymentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    void getPaymentById_shouldReturn404WhenNotFound() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.getPaymentById(paymentId))
                .thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void refundPayment_shouldReturn200() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("99.99"), "TRY",
                PaymentMethod.CREDIT_CARD, PaymentStatus.REFUNDED,
                null, null, Instant.now(),
                List.of(), Instant.now(), Instant.now()
        );

        when(paymentService.refundPayment(any(UUID.class), any(RefundRequest.class))).thenReturn(response);

        RefundRequest request = new RefundRequest("Customer requested");
        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void refundPayment_shouldReturn422WhenPaymentNotCompleted() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.refundPayment(any(UUID.class), any(RefundRequest.class)))
                .thenThrow(new PaymentRefundException(paymentId, "Only completed payments can be refunded"));

        RefundRequest request = new RefundRequest("Reason");
        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refundPayment_shouldReturn400WhenReasonBlank() throws Exception {
        RefundRequest request = new RefundRequest("");
        mockMvc.perform(post("/api/v1/payments/{id}/refund", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_shouldReturn201WithLocationHeader() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("149.99"), "TRY",
                PaymentMethod.CREDIT_CARD, PaymentStatus.COMPLETED,
                "MOCK-REF-1", null, Instant.now(),
                List.of(), Instant.now(), Instant.now()
        );

        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("149.99"), "TRY", PaymentMethod.CREDIT_CARD
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/" + paymentId))
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createPayment_shouldReturn400WhenRequestInvalid() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "", null, null, new BigDecimal("-5"), "", null
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_shouldReturn409WhenDuplicateRequest() throws Exception {
        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenThrow(new DuplicateRequestException("req-1"));

        CreatePaymentRequest request = new CreatePaymentRequest(
                "req-1", UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("149.99"), "TRY", PaymentMethod.CREDIT_CARD
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
