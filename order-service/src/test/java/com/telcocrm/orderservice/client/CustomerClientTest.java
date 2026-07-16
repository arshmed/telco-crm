package com.telcocrm.orderservice.client;

import com.telcocrm.orderservice.client.dto.CustomerResponse;
import com.telcocrm.orderservice.exception.CustomerNotFoundException;
import com.telcocrm.orderservice.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerClientTest {

    private final CustomerClient client = new CustomerClient() {
        @Override
        public CustomerResponse getCustomerById(UUID id) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public CustomerResponse getCustomerByNo(String customerNo) {
            throw new UnsupportedOperationException("not used in this test");
        }
    };

    private Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/api/v1/customers/1", Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void shouldThrowCustomerNotFoundWhenUpstreamReturns404() {
        var id = UUID.randomUUID();
        var notFound = new FeignException.NotFound("not found", dummyRequest(), null, null);

        assertThatThrownBy(() -> client.getCustomerByIdFallback(id, notFound))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void shouldThrowServiceUnavailableForOtherFailures() {
        var id = UUID.randomUUID();
        var cause = new RuntimeException("connection reset");

        assertThatThrownBy(() -> client.getCustomerByIdFallback(id, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Customer service");
    }
}
