package com.telcocrm.customerservice.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @Test
    void shouldBuildWithAllFields() {
        var id = UUID.randomUUID();
        var address = Address.builder()
                .id(id)
                .line1("Line 1")
                .city("City")
                .district("District")
                .build();
        assertThat(address.getId()).isEqualTo(id);
        assertThat(address.getLine1()).isEqualTo("Line 1");
        assertThat(address.getCity()).isEqualTo("City");
        assertThat(address.getDistrict()).isEqualTo("District");
    }

    @Test
    void shouldUseDefaultIsDefault() {
        var address = Address.builder().build();
        assertThat(address.getIsDefault()).isFalse();
    }
}
