package com.telcocrm.customerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telcocrm.customerservice.dto.CustomerRequest;
import com.telcocrm.customerservice.dto.CustomerResponse;
import com.telcocrm.customerservice.dto.DocumentResponse;
import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.enums.CustomerType;
import com.telcocrm.customerservice.exception.GlobalExceptionHandler;
import com.telcocrm.customerservice.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(
                new org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule(
                        new SpringDataWebSettings(
                                EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)));
    }

    private CustomerResponse sampleResponse() {
        return CustomerResponse.builder()
                .id(UUID.randomUUID())
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("5551234567")
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        var controller = new CustomerController(customerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createCustomer_shouldReturn201() throws Exception {
        var request = CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("5551234567")
                .build();
        when(customerService.createCustomer(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void createCustomer_shouldReturn400WhenInvalid() throws Exception {
        var request = CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("")
                .lastName("")
                .identityNumber("")
                .dateOfBirth(null)
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCustomers_shouldReturn200() throws Exception {
        when(customerService.listCustomers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    void getCustomer_shouldReturn200() throws Exception {
        var response = sampleResponse();
        when(customerService.getCustomer(response.getId())).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()));
    }

    @Test
    void updateCustomer_shouldReturn200() throws Exception {
        var request = CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("Jane")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        var response = sampleResponse();
        response.setFirstName("Jane");
        when(customerService.updateCustomer(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/customers/{id}", response.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void deleteCustomer_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void approveKyc_shouldReturn200() throws Exception {
        var response = sampleResponse();
        when(customerService.approveKyc(response.getId())).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/{id}/kyc/approve", response.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void rejectKyc_shouldReturn200() throws Exception {
        var response = sampleResponse();
        when(customerService.rejectKyc(response.getId())).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/{id}/kyc/reject", response.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void addDocument_shouldReturn201() throws Exception {
        var id = UUID.randomUUID();
        var docRequest = """
                {"type":"ID_CARD","fileRef":"ref-123"}
                """;
        when(customerService.addDocument(any(), any())).thenReturn(
                DocumentResponse.builder()
                        .id(UUID.randomUUID())
                        .type("ID_CARD")
                        .fileRef("ref-123")
                        .build());

        mockMvc.perform(post("/api/v1/customers/{id}/documents", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(docRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileRef").value("ref-123"));
    }
}
