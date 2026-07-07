package com.telcocrm.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.orderservice.dto.request.CancelOrderRequest;
import com.telcocrm.orderservice.dto.request.CreateOrderRequest;
import com.telcocrm.orderservice.dto.request.OrderItemRequest;
import com.telcocrm.orderservice.dto.response.OrderResponse;
import com.telcocrm.orderservice.entity.enums.OrderItemType;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.exception.GlobalExceptionHandler;
import com.telcocrm.orderservice.exception.OrderNotCancellableException;
import com.telcocrm.orderservice.exception.OrderNotFoundException;
import com.telcocrm.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(
                new SpringDataJacksonConfiguration.PageModule(
                        new SpringDataWebSettings(
                                EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));
    }

    @BeforeEach
    void setUp() {
        var controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private OrderResponse sampleResponse() {
        return new OrderResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderStatus.PENDING_PAYMENT,
                BigDecimal.TEN,
                "TRY",
                null,
                null,
                null,
                List.of(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createOrder_shouldReturn201() throws Exception {
        var request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 1))
        );
        var response = sampleResponse();
        when(orderService.createOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/" + response.id()))
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    void createOrder_shouldReturn400WhenNoItems() throws Exception {
        var request = new CreateOrderRequest(UUID.randomUUID(), List.of());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400WhenCustomerIdMissing() throws Exception {
        var request = new CreateOrderRequest(
                null,
                List.of(new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 1))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn409WhenCustomerNotActive() throws Exception {
        var request = new CreateOrderRequest(
                UUID.randomUUID(),
                List.of(new OrderItemRequest("TARIFF-1", OrderItemType.TARIFF, 1))
        );
        when(orderService.createOrder(any(), any())).thenThrow(new IllegalStateException("Customer " + request.customerId() + " is not active"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listOrders_shouldReturn200() throws Exception {
        when(orderService.listOrders(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency").value("TRY"));
    }

    @Test
    void getOrderById_shouldReturn200() throws Exception {
        var response = sampleResponse();
        when(orderService.getOrderById(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{orderId}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void getOrderById_shouldReturn404WhenNotFound() throws Exception {
        var orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId)).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_shouldReturn200() throws Exception {
        var request = new CancelOrderRequest("customer changed mind");
        var response = sampleResponse();
        when(orderService.cancelOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("TRY"));
    }

    @Test
    void cancelOrder_shouldReturn400WhenReasonBlank() throws Exception {
        var request = new CancelOrderRequest("");

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_shouldReturn404WhenOrderNotFound() throws Exception {
        var orderId = UUID.randomUUID();
        var request = new CancelOrderRequest("customer changed mind");
        when(orderService.cancelOrder(any(), any())).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_shouldReturn422WhenOrderNotCancellable() throws Exception {
        var orderId = UUID.randomUUID();
        var request = new CancelOrderRequest("customer changed mind");
        when(orderService.cancelOrder(any(), any())).thenThrow(new OrderNotCancellableException(orderId, OrderStatus.FULFILLED));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422));
    }
}
