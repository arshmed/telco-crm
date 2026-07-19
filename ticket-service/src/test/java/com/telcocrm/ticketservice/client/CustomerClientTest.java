package com.telcocrm.ticketservice.client;

import com.telcocrm.ticketservice.client.dto.CustomerResponse;
import com.telcocrm.ticketservice.exception.CustomerNotFoundException;
import com.telcocrm.ticketservice.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerClientTest {

    private final CustomerClient client = new CustomerClient() {
        @Override
        public CustomerResponse getCustomerById(UUID id) {
            throw new UnsupportedOperationException("not used in fallback tests");
        }
    };

    private final UUID customerId = UUID.randomUUID();

    @Test
    void shouldThrowCustomerNotFoundWhenUpstreamReturns404() {
        assertThatThrownBy(() -> client.getCustomerByIdFallback(customerId, notFoundException()))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void shouldThrowServiceUnavailableWhenCircuitIsOpen() {
        assertThatThrownBy(() -> client.getCustomerByIdFallback(customerId, new RuntimeException("circuit open")))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Customer service");
    }

    private FeignException notFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/customers/" + customerId,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("CustomerClient#getCustomerById(UUID)",
                feign.Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(request)
                        .headers(Collections.emptyMap())
                        .build());
    }
}
