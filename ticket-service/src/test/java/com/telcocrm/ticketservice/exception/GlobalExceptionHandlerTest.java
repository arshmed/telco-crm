package com.telcocrm.ticketservice.exception;

import com.telcocrm.ticketservice.entity.enums.TicketStatus;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapTicketNotFoundTo404() {
        UUID ticketId = UUID.randomUUID();

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(new TicketNotFoundException(ticketId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TICKET_NOT_FOUND");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "TICKET_NOT_FOUND");
        assertThat(response.getBody().getType().toString()).isEqualTo("https://telcocrm.com/errors/ticket-not-found");
    }

    @Test
    void shouldMapTicketNotModifiableTo409() {
        var ex = new TicketNotModifiableException(UUID.randomUUID(), TicketStatus.RESOLVED, "resolved");

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("TICKET_NOT_MODIFIABLE");
    }

    @Test
    void shouldMapServiceUnavailableTo503() {
        var ex = new ServiceUnavailableException("Customer service", "CUSTOMER_SERVICE_UNAVAILABLE",
                new RuntimeException("timeout"));

        ResponseEntity<ProblemDetail> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getTitle()).isEqualTo("CUSTOMER_SERVICE_UNAVAILABLE");
    }

    @Test
    void shouldMapAccessDeniedTo403() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getTitle()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void shouldMapOptimisticLockingTo409() {
        var ex = new ObjectOptimisticLockingFailureException("tickets", UUID.randomUUID());

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailureException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("CONCURRENT_UPDATE");
    }

    @Test
    void shouldMapIllegalStateTo409() {
        ResponseEntity<ProblemDetail> response = handler.handleIllegalStateException(
                new IllegalStateException("Customer is not active"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getDetail()).isEqualTo("Customer is not active");
    }

    @Test
    void shouldMapMalformedBodyTo400() {
        ResponseEntity<ProblemDetail> response = handler.handleMessageNotReadableException(
                new HttpMessageNotReadableException("bad json",
                        new MockHttpInputMessage("{".getBytes(StandardCharsets.UTF_8))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void shouldMapFeignNotFoundTo404() {
        ResponseEntity<ProblemDetail> response = handler.handleFeignException(feignException(404));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("UPSTREAM_SERVICE_ERROR");
    }

    @Test
    void shouldMapOtherFeignErrorsTo503() {
        ResponseEntity<ProblemDetail> response = handler.handleFeignException(feignException(500));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldMapUnexpectedErrorTo500() {
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getDetail()).isEqualTo("Unexpected error occurred");
    }

    private FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/customers/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return FeignException.errorStatus("CustomerClient#getCustomerById(UUID)",
                feign.Response.builder()
                        .status(status)
                        .reason("error")
                        .request(request)
                        .headers(Collections.emptyMap())
                        .build());
    }
}
