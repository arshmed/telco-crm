package com.telcocrm.customerservice.service;

import com.telcocrm.customerservice.config.CustomerAuditListener;
import com.telcocrm.customerservice.dto.*;
import com.telcocrm.customerservice.entity.Address;
import com.telcocrm.customerservice.entity.Customer;
import com.telcocrm.customerservice.entity.Document;
import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.enums.CustomerType;
import com.telcocrm.customerservice.enums.DocumentType;
import com.telcocrm.customerservice.exception.DuplicateResourceException;
import com.telcocrm.customerservice.exception.ResourceNotFoundException;
import com.telcocrm.customerservice.mapper.CustomerMapper;
import com.telcocrm.customerservice.repository.CustomerRepository;
import com.telcocrm.customerservice.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private CustomerAuditListener auditListener;

    @InjectMocks
    private CustomerService customerService;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    private CustomerRequest validRequest() {
        return CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("5551234567")
                .build();
    }

    private Customer validCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("5551234567")
                .status(CustomerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CustomerResponse validResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .type(customer.getType())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .identityNumber(customer.getIdentityNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    @Test
    void shouldCreateCustomer() {
        var request = validRequest();
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.existsByIdentityNumberHash(any())).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.createCustomer(request);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(outboxService).saveEvent(eq("CUSTOMER"), any(), eq("customer-registered-topic"), any());
        verify(auditListener).logCreate(eq("Customer"), any(), any());
    }

    @Test
    void shouldThrowWhenIdentityNumberExists() {
        var request = validRequest();
        when(customerRepository.existsByIdentityNumberHash(any())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("identityNumber");
        verify(customerRepository, never()).save(any());
        verify(outboxService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void shouldCreateCustomerWithAddresses() {
        var request = validRequest();
        request.setAddresses(List.of(
                AddressRequest.builder().line1("Line 1").city("City").district("District").build()
        ));
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.existsByIdentityNumberHash(any())).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerMapper.toEntity(any(AddressRequest.class))).thenReturn(
                Address.builder().line1("Line 1").city("City").district("District").build());
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.createCustomer(request);

        assertThat(result).isNotNull();
        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getAddresses()).hasSize(1);
    }

    @Test
    void shouldListCustomers() {
        var customer = validCustomer();
        var response = validResponse(customer);
        var page = new PageImpl<>(List.of(customer));

        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        Page<CustomerResponse> result = customerService.listCustomers(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldGetCustomerById() {
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.getCustomer(customer.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customer.getId());
    }

    @Test
    void shouldThrowWhenCustomerNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateCustomer() {
        var request = validRequest();
        request.setFirstName("Jane");
        var customer = validCustomer();
        var updatedCustomer = validCustomer();
        updatedCustomer.setFirstName("Jane");
        var response = validResponse(updatedCustomer);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(updatedCustomer);
        when(customerMapper.toResponse(updatedCustomer)).thenReturn(response);

        CustomerResponse result = customerService.updateCustomer(customer.getId(), request);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        verify(outboxService).saveEvent(eq("CUSTOMER"), any(), eq("customer-updated-topic"), any());
        verify(auditListener).logUpdate(eq("Customer"), any(), any(), any());
    }

    @Test
    void shouldThrowOnUpdateWhenIdentityNumberConflicts() {
        var request = validRequest();
        request.setIdentityNumber("99999999999");
        var customer = validCustomer();

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.existsByIdentityNumberHash(any())).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(customer.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void shouldDeleteCustomer() {
        var customer = validCustomer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customer.getId());

        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getDeletedAt()).isNotNull();
        verify(auditListener).logDelete(eq("Customer"), any(), any());
    }

    @Test
    void shouldAddDocument() {
        var customer = validCustomer();
        var request = DocumentRequest.builder().type(DocumentType.ID_CARD).fileRef("ref-123").build();
        var document = Document.builder().id(UUID.randomUUID()).type(DocumentType.ID_CARD).fileRef("ref-123").build();
        var docResponse = DocumentResponse.builder().id(document.getId()).type(DocumentType.ID_CARD).fileRef("ref-123").build();

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerMapper.toEntity(request)).thenReturn(document);
        when(documentRepository.save(any())).thenReturn(document);
        when(customerMapper.toDocumentResponse(document)).thenReturn(docResponse);

        DocumentResponse result = customerService.addDocument(customer.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getFileRef()).isEqualTo("ref-123");
    }

    @Test
    void shouldApproveKyc() {
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.approveKyc(customer.getId());

        assertThat(result).isNotNull();
        verify(outboxService).saveEvent(eq("CUSTOMER"), any(), eq("customer-kyc-approved-topic"), any());
    }

    @Test
    void shouldThrowWhenKycNotPending() {
        var customer = validCustomer();
        customer.setStatus(CustomerStatus.ACTIVE);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.approveKyc(customer.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectKyc() {
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = customerService.rejectKyc(customer.getId());

        assertThat(result).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.REJECTED);
        verify(outboxService).saveEvent(eq("CUSTOMER"), any(), eq("customer-kyc-rejected-topic"), any());
    }

    @Test
    void shouldThrowWhenRejectKycNotPending() {
        var customer = validCustomer();
        customer.setStatus(CustomerStatus.ACTIVE);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.rejectKyc(customer.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUpdateWithAddresses() {
        var request = validRequest();
        request.setAddresses(List.of(
                AddressRequest.builder().line1("New Line").city("New City").district("New District").build()
        ));
        var customer = validCustomer();
        var response = validResponse(customer);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);
        when(customerMapper.toEntity(any(AddressRequest.class))).thenReturn(
                Address.builder().line1("New Line").city("New City").district("New District").build());

        CustomerResponse result = customerService.updateCustomer(customer.getId(), request);

        assertThat(result).isNotNull();
        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getAddresses()).hasSize(1);
    }

    @Test
    void shouldSetDocumentsVerifiedOnKycApprove() {
        var customer = validCustomer();
        var doc = Document.builder().id(UUID.randomUUID()).type(DocumentType.ID_CARD).fileRef("ref-1").build();
        customer.getDocuments().add(doc);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(validResponse(customer));

        customerService.approveKyc(customer.getId());

        assertThat(doc.getVerifiedAt()).isNotNull();
    }
}
