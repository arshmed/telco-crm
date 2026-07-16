package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAuditLog;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import com.telcocrm.paymentservice.entity.enums.PaymentStatus;
import com.telcocrm.paymentservice.repository.PaymentAuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentAuditServiceTest {

    @Mock
    private PaymentAuditLogRepository paymentAuditLogRepository;

    @InjectMocks
    private PaymentAuditService paymentAuditService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_shouldUsePreferredUsernameClaimWhenPresent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "testadmin")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));

        Payment payment = buildPayment();
        paymentAuditService.log(payment, "did something");

        PaymentAuditLog entry = captureSavedEntry();
        assertThat(entry.getPerformedBy()).isEqualTo("testadmin");
        assertThat(entry.getPaymentId()).isEqualTo(payment.getId());
        assertThat(entry.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(entry.getDetail()).isEqualTo("did something");
    }

    @Test
    void log_shouldFallBackToAuthenticationNameWhenNoPreferredUsernameClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-sub-123")
                .claim("scope", "email")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));

        paymentAuditService.log(buildPayment(), "detail");

        assertThat(captureSavedEntry().getPerformedBy()).isEqualTo("user-sub-123");
    }

    @Test
    void log_shouldUseSystemWhenNoAuthenticationPresent() {
        paymentAuditService.log(buildPayment(), "detail");

        assertThat(captureSavedEntry().getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void log_shouldUseSystemWhenAuthenticationIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        paymentAuditService.log(buildPayment(), "detail");

        assertThat(captureSavedEntry().getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void log_shouldUseSystemWhenAuthenticationIsNotAuthenticated() {
        TestingAuthenticationToken unauthenticated = new TestingAuthenticationToken("user", "pw");
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        paymentAuditService.log(buildPayment(), "detail");

        assertThat(captureSavedEntry().getPerformedBy()).isEqualTo("SYSTEM");
    }

    private PaymentAuditLog captureSavedEntry() {
        ArgumentCaptor<PaymentAuditLog> captor = ArgumentCaptor.forClass(PaymentAuditLog.class);
        verify(paymentAuditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private Payment buildPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .currency("TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }
}
