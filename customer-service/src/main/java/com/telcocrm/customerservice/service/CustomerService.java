package com.telcocrm.customerservice.service;

import com.telcocrm.customerservice.config.CustomerAuditListener;
import com.telcocrm.customerservice.dto.*;
import com.telcocrm.customerservice.entity.Address;
import com.telcocrm.customerservice.entity.Customer;
import com.telcocrm.customerservice.entity.Document;
import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.enums.CustomerType;
import com.telcocrm.customerservice.event.CustomerKYCApprovedEvent;
import com.telcocrm.customerservice.event.CustomerKYCRejectedEvent;
import com.telcocrm.customerservice.event.CustomerRegisteredEvent;
import com.telcocrm.customerservice.event.CustomerUpdatedEvent;
import com.telcocrm.customerservice.exception.DuplicateResourceException;
import com.telcocrm.customerservice.exception.ResourceNotFoundException;
import com.telcocrm.customerservice.mapper.CustomerMapper;
import com.telcocrm.customerservice.repository.CustomerRepository;
import com.telcocrm.customerservice.repository.DocumentRepository;
import com.telcocrm.customerservice.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final CustomerMapper customerMapper;
    private final OutboxService outboxService;
    private final CustomerAuditListener auditListener;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateCustomerRequest(request);
        String identityHash = hashIdentityNumber(request.getIdentityNumber());
        if (customerRepository.existsByIdentityNumberHash(identityHash)) {
            throw new DuplicateResourceException("Customer", "identityNumber", request.getIdentityNumber());
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setIdentityNumberHash(identityHash);
        customer.setCustomerNo(generateCustomerNo());

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
            "customer-registered-topic",
            CustomerRegisteredEvent.of(
                saved.getId(),
                saved.getType(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getIdentityNumber(),
                saved.getEmail()
            )
        );

        auditListener.logCreate("Customer", saved.getId().toString(), saved);

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

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByNo(String customerNo) {
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerNo", customerNo));
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        validateCustomerRequest(request);
        Customer customer = findCustomerById(id);

        String newHash = hashIdentityNumber(request.getIdentityNumber());
        if (!request.getIdentityNumber().equals(customer.getIdentityNumber())
                && customerRepository.existsByIdentityNumberHash(newHash)) {
            throw new DuplicateResourceException("Customer", "identityNumber", request.getIdentityNumber());
        }

        customerMapper.updateEntity(customer, request);
        customer.setIdentityNumberHash(newHash);

        if (request.getAddresses() != null) {
            customer.getAddresses().clear();
            List<Address> addresses = request.getAddresses().stream()
                    .map(addrReq -> {
                        Address address = customerMapper.toEntity(addrReq);
                        address.setCustomer(customer);
                        return address;
                    })
                    .toList();
            customer.getAddresses().addAll(addresses);
        }

        Customer saved = customerRepository.save(customer);

        outboxService.saveEvent(
            "CUSTOMER",
            saved.getId().toString(),
            "customer-updated-topic",
            CustomerUpdatedEvent.of(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getPhone()
            )
        );

        auditListener.logUpdate("Customer", saved.getId().toString(), customer, saved);

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = findCustomerById(id);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
        auditListener.logDelete("Customer", customer.getId().toString(), customer);
    }

    @Transactional
    public DocumentResponse addDocument(UUID customerId, DocumentRequest request) {
        Customer customer = findCustomerById(customerId);

        if (!documentTypeRepository.findById(request.getType()).isPresent()) {
            throw new IllegalArgumentException("Invalid document type: " + request.getType());
        }

        Document document = customerMapper.toEntity(request);
        document.setCustomer(customer);

        Document saved = documentRepository.save(document);
        customer.getDocuments().add(saved);

        return customerMapper.toDocumentResponse(saved);
    }

    @Transactional
    public CustomerResponse rejectKyc(UUID id) {
        Customer customer = findCustomerById(id);
        if (customer.getStatus() != CustomerStatus.PENDING) {
            throw new IllegalArgumentException("KYC can only be rejected when customer status is PENDING");
        }
        customer.setStatus(CustomerStatus.REJECTED);
        Customer saved = customerRepository.save(customer);

        outboxService.saveEvent(
            "CUSTOMER",
            saved.getId().toString(),
            "customer-kyc-rejected-topic",
            CustomerKYCRejectedEvent.of(saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getEmail())
        );

        return customerMapper.toResponse(saved);
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
            "customer-kyc-approved-topic",
            CustomerKYCApprovedEvent.of(saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getEmail())
        );

        return customerMapper.toResponse(saved);
    }

    private void validateCustomerRequest(CustomerRequest request) {
        if (request.getType() == CustomerType.CORPORATE) {
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                throw new IllegalArgumentException("companyName is required for corporate customers");
            }
            if (request.getTaxOffice() == null || request.getTaxOffice().isBlank()) {
                throw new IllegalArgumentException("taxOffice is required for corporate customers");
            }
            if (request.getIdentityNumber().length() != 10) {
                throw new IllegalArgumentException("VKN must be 10 digits for corporate customers");
            }
        } else if (request.getType() == CustomerType.INDIVIDUAL) {
            if (request.getIdentityNumber().length() != 11) {
                throw new IllegalArgumentException("TCKN must be 11 digits for individual customers");
            }
        }
    }

    private Customer findCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    private String hashIdentityNumber(String identityNumber) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(identityNumber.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String generateCustomerNo() {
        String maxNo = customerRepository.findMaxCustomerNo().orElse("C-000000");
        long nextNum = Long.parseLong(maxNo.substring(2)) + 1;
        return String.format("C-%06d", nextNum);
    }
}
