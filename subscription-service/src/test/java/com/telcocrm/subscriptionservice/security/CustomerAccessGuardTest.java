package com.telcocrm.subscriptionservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerAccessGuardTest {

    private final CustomerAccessGuard guard = new CustomerAccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String customerId, String... authorities) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (customerId != null) {
            builder.claim("customer_id", customerId);
        }
        Jwt jwt = builder.build();
        List<GrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        var token = new JwtAuthenticationToken(jwt, grantedAuthorities, "user-1");
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void assertOwnResource_allowsWhenCustomerIdMatches() {
        var customerId = UUID.randomUUID();
        authenticateAs(customerId.toString(), "CUSTOMER");

        guard.assertOwnResource(customerId, IllegalStateException::new);
    }

    @Test
    void assertOwnResource_deniesWhenCustomerIdDoesNotMatch() {
        var ownId = UUID.randomUUID();
        var otherId = UUID.randomUUID();
        authenticateAs(ownId.toString(), "CUSTOMER");

        assertThatThrownBy(() -> guard.assertOwnResource(otherId, () -> new IllegalStateException("denied")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("denied");
    }

    @Test
    void assertOwnResource_deniesWhenCustomerIdClaimMissing() {
        var otherId = UUID.randomUUID();
        authenticateAs(null, "CUSTOMER");

        assertThatThrownBy(() -> guard.assertOwnResource(otherId, () -> new IllegalStateException("denied")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assertOwnResource_allowsAnyResourceForStaffRole() {
        var otherId = UUID.randomUUID();
        authenticateAs(UUID.randomUUID().toString(), "CUSTOMER", "MARKETING_MANAGER");

        guard.assertOwnResource(otherId, IllegalStateException::new);
    }

    @Test
    void assertOwnResource_allowsAnyResourceForAdminRoleWithoutCustomerAuthority() {
        var otherId = UUID.randomUUID();
        authenticateAs(null, "SYSTEM_ADMIN");

        guard.assertOwnResource(otherId, IllegalStateException::new);
    }

    @Test
    void effectiveCustomerFilter_forcesOwnCustomerIdWhenCustomerOnly() {
        var ownId = UUID.randomUUID();
        authenticateAs(ownId.toString(), "CUSTOMER");

        UUID filter = guard.effectiveCustomerFilter(UUID.randomUUID());

        assertThat(filter).isEqualTo(ownId);
    }

    @Test
    void effectiveCustomerFilter_keepsRequestedFilterForStaffRole() {
        var requested = UUID.randomUUID();
        authenticateAs(null, "BILLING_OPERATOR");

        UUID filter = guard.effectiveCustomerFilter(requested);

        assertThat(filter).isEqualTo(requested);
    }

    @Test
    void effectiveCustomerFilter_keepsNullFilterForStaffRole() {
        authenticateAs(null, "CALL_CENTER_AGENT");

        UUID filter = guard.effectiveCustomerFilter(null);

        assertThat(filter).isNull();
    }
}
