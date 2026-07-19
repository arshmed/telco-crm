package com.telcocrm.customerservice.entity;

import com.telcocrm.customerservice.enums.CustomerStatus;
import com.telcocrm.customerservice.enums.CustomerType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var customer = Customer.builder()
                .id(id)
                .type(CustomerType.INDIVIDUAL)
                .firstName("John")
                .build();
        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(customer.getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldUseDefaultStatus() {
        var customer = Customer.builder().build();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.PENDING);
    }
}
