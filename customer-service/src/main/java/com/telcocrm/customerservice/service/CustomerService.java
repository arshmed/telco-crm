package com.telcocrm.customerservice.service;

import com.telcocrm.customerservice.dto.*;
import com.telcocrm.customerservice.entity.Address;
import com.telcocrm.customerservice.entity.Customer;
import com.telcocrm.customerservice.entity.Document;
import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.event.CustomerKYCApprovedEvent;
import com.telcocrm.customerservice.event.CustomerRegisteredEvent;
import com.telcocrm.customerservice.event.CustomerUpdatedEvent;
import com.telcocrm.customerservice.exception.DuplicateResourceException;
import com.telcocrm.customerservice.exception.ResourceNotFoundException;
import com.telcocrm.customerservice.mapper.CustomerMapper;
import com.telcocrm.customerservice.repository.AddressRepository;
import com.telcocrm.customerservice.repository.CustomerRepository;
import com.telcocrm.customerservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final DocumentRepository documentRepository;
    private final CustomerMapper customerMapper;
    private final OutboxService outboxService;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new DuplicateResourceException("Customer", "identityNumber", request.getIdentityNumber());
        }

        Customer customer = customerMapper.toEntity(request);

        if (request.getAddresses() != null) {
            List<Address> addresses = request.getAddresses().stream()
                    .map(addrReq -> {
                        Address address = customerMapper.toEntity(addrReq);
                        address.setCustomer(customer);
                        return address;
                    })
                    .collect(Collectors.toList());
            customer.setAddresses(addresses);
        }

        Customer saved = customerRepository.save(customer);

        outboxService.saveEvent(
            "CUSTOMER",
            saved.getId().toString(),
            "customerRegisteredEvent",
            CustomerRegisteredEvent.of(
                saved.getId(),
                saved.getType(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getIdentityNumber()
            )
        );

        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        return customerMapper.toResponse(findCustomerById(id));
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = findCustomerById(id);

        if (!customer.getIdentityNumber().equals(request.getIdentityNumber())
                && customerRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new DuplicateResourceException("Customer", "identityNumber", request.getIdentityNumber());
        }

        customerMapper.updateEntity(customer, request);

        if (request.getAddresses() != null) {
            customer.getAddresses().clear();
            List<Address> addresses = request.getAddresses().stream()
                    .map(addrReq -> {
                        Address address = customerMapper.toEntity(addrReq);
                        address.setCustomer(customer);
                        return address;
                    })
                    .collect(Collectors.toList());
            customer.getAddresses().addAll(addresses);
        }

        Customer saved = customerRepository.save(customer);

        outboxService.saveEvent(
            "CUSTOMER",
            saved.getId().toString(),
            "customerUpdatedEvent",
            CustomerUpdatedEvent.of(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhone()
            )
        );

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = findCustomerById(id);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Transactional
    public DocumentResponse addDocument(UUID customerId, DocumentRequest request) {
        Customer customer = findCustomerById(customerId);

        Document document = customerMapper.toEntity(request);
        document.setCustomer(customer);

        Document saved = documentRepository.save(document);
        customer.getDocuments().add(saved);

        return customerMapper.toDocumentResponse(saved);
    }

    @Transactional
    public CustomerResponse approveKyc(UUID id) {
        Customer customer = findCustomerById(id);
        if (customer.getStatus() != CustomerStatus.PENDING) {
            throw new IllegalArgumentException("KYC can only be approved when customer status is PENDING");
        }

        customer.setStatus(CustomerStatus.ACTIVE);

        LocalDateTime now = LocalDateTime.now();
        customer.getDocuments().stream()
            .filter(doc -> doc.getVerifiedAt() == null)
            .forEach(doc -> doc.setVerifiedAt(now));

        Customer saved = customerRepository.save(customer);

        outboxService.saveEvent(
            "CUSTOMER",
            saved.getId().toString(),
            "customerKYCApprovedEvent",
            CustomerKYCApprovedEvent.of(saved.getId(), saved.getFirstName(), saved.getLastName())
        );

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse rejectKyc(UUID id) {
        Customer customer = findCustomerById(id);
        if (customer.getStatus() != CustomerStatus.PENDING) {
            throw new IllegalArgumentException("KYC can only be rejected when customer status is PENDING");
        }
        customer.setStatus(CustomerStatus.REJECTED);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    private Customer findCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }
}
