package com.telcocrm.customerservice.mapper;

import com.telcocrm.customerservice.dto.*;
import com.telcocrm.customerservice.entity.Address;
import com.telcocrm.customerservice.entity.Customer;
import com.telcocrm.customerservice.entity.Document;
import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.enums.CustomerType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapperImpl();

    private Customer aCustomer() {
        return Customer.builder()
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
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Address anAddress(Customer customer) {
        return Address.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .line1("Line 1")
                .city("City")
                .district("District")
                .postalCode("34000")
                .isDefault(true)
                .build();
    }

    private Document aDocument(Customer customer) {
        return Document.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .type("ID_CARD")
                .fileRef("ref-123")
                .build();
    }

    @Test
    void toResponse_shouldMapAllFields() {
        var customer = aCustomer();
        var doc = aDocument(customer);
        var address = anAddress(customer);
        customer.getDocuments().add(doc);
        customer.getAddresses().add(address);

        var response = mapper.toResponse(customer);

        assertThat(response.getId()).isEqualTo(customer.getId());
        assertThat(response.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getIdentityNumber()).isEqualTo("12345678901");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getPhone()).isEqualTo("5551234567");
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(response.getAddresses()).hasSize(1);
        assertThat(response.getDocuments()).hasSize(1);
    }

    @Test
    void toResponse_shouldReturnNullWhenNull() {
        assertThat(mapper.toResponse((Customer) null)).isNull();
    }

    @Test
    void toResponseList_shouldMapList() {
        var customer = aCustomer();
        var result = mapper.toResponseList(List.of(customer));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(customer.getId());
    }

    @Test
    void toResponseList_shouldReturnNullWhenNull() {
        assertThat(mapper.toResponseList(null)).isNull();
    }

    @Test
    void toAddressResponse_shouldMapAllFields() {
        var customer = aCustomer();
        var address = anAddress(customer);
        var response = mapper.toAddressResponse(address);
        assertThat(response.getId()).isEqualTo(address.getId());
        assertThat(response.getLine1()).isEqualTo("Line 1");
        assertThat(response.getCity()).isEqualTo("City");
        assertThat(response.getDistrict()).isEqualTo("District");
        assertThat(response.getPostalCode()).isEqualTo("34000");
        assertThat(response.getIsDefault()).isTrue();
    }

    @Test
    void toAddressResponse_shouldReturnNullWhenNull() {
        assertThat(mapper.toAddressResponse(null)).isNull();
    }

    @Test
    void toDocumentResponse_shouldMapAllFields() {
        var customer = aCustomer();
        var doc = aDocument(customer);
        var response = mapper.toDocumentResponse(doc);
        assertThat(response.getId()).isEqualTo(doc.getId());
        assertThat(response.getType()).isEqualTo("ID_CARD");
        assertThat(response.getFileRef()).isEqualTo("ref-123");
    }

    @Test
    void toDocumentResponse_shouldReturnNullWhenNull() {
        assertThat(mapper.toDocumentResponse(null)).isNull();
    }

    @Test
    void toEntity_shouldMapCustomerRequest() {
        var request = CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .lastName("Doe")
                .identityNumber("12345678901")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("5551234567")
                .build();

        var customer = mapper.toEntity(request);

        assertThat(customer.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getIdentityNumber()).isEqualTo("12345678901");
        assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getPhone()).isEqualTo("5551234567");
        assertThat(customer.getId()).isNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING);
    }

    @Test
    void toEntity_shouldReturnNullWhenCustomerRequestNull() {
        assertThat(mapper.toEntity((CustomerRequest) null)).isNull();
    }

    @Test
    void toEntity_shouldMapAddressRequest() {
        var request = AddressRequest.builder()
                .line1("Line 1")
                .city("City")
                .district("District")
                .postalCode("34000")
                .isDefault(true)
                .build();

        var address = mapper.toEntity(request);

        assertThat(address.getLine1()).isEqualTo("Line 1");
        assertThat(address.getCity()).isEqualTo("City");
        assertThat(address.getDistrict()).isEqualTo("District");
        assertThat(address.getPostalCode()).isEqualTo("34000");
        assertThat(address.getIsDefault()).isTrue();
        assertThat(address.getId()).isNull();
    }

    @Test
    void toEntity_shouldReturnNullWhenAddressRequestNull() {
        assertThat(mapper.toEntity((AddressRequest) null)).isNull();
    }

    @Test
    void toEntity_shouldMapDocumentRequest() {
        var request = DocumentRequest.builder()
                .type("ID_CARD")
                .fileRef("ref-123")
                .build();

        var doc = mapper.toEntity(request);

        assertThat(doc.getType()).isEqualTo("ID_CARD");
        assertThat(doc.getFileRef()).isEqualTo("ref-123");
        assertThat(doc.getId()).isNull();
    }

    @Test
    void toEntity_shouldReturnNullWhenDocumentRequestNull() {
        assertThat(mapper.toEntity((DocumentRequest) null)).isNull();
    }

    @Test
    void updateEntity_shouldUpdateFields() {
        var customer = aCustomer();
        var request = CustomerRequest.builder()
                .type(CustomerType.INDIVIDUAL)
                .firstName("Jane")
                .lastName("Doe")
                .identityNumber("99999999999")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .email("jane@example.com")
                .phone("5559999999")
                .build();

        mapper.updateEntity(customer, request);

        assertThat(customer.getFirstName()).isEqualTo("Jane");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getIdentityNumber()).isEqualTo("99999999999");
        assertThat(customer.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");
        assertThat(customer.getPhone()).isEqualTo("5559999999");
    }

    @Test
    void updateEntity_shouldDoNothingWhenRequestNull() {
        var customer = aCustomer();
        mapper.updateEntity(customer, null);
        assertThat(customer.getFirstName()).isEqualTo("John");
    }
}
