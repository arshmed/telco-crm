package com.telcocrm.usageservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class CustomerAccessGuard {

    private static final String CUSTOMER_AUTHORITY = "CUSTOMER";
    private static final Set<String> STAFF_AUTHORITIES = Set.of(
            "CALL_CENTER_AGENT", "FIELD_DEALER", "MARKETING_MANAGER", "SYSTEM_ADMIN", "BILLING_OPERATOR");
    private static final UUID UNRESOLVABLE_CUSTOMER_ID = new UUID(0L, 0L);

    public void assertOwnResource(UUID resourceCustomerId, Supplier<? extends RuntimeException> notFoundSupplier) {
        if (!isCustomerOnly()) {
            return;
        }
        UUID ownCustomerId = ownCustomerId().orElse(UNRESOLVABLE_CUSTOMER_ID);
        if (resourceCustomerId == null || !ownCustomerId.equals(resourceCustomerId)) {
            throw notFoundSupplier.get();
        }
    }

    public UUID effectiveCustomerFilter(UUID requestedCustomerId) {
        return isCustomerOnly() ? ownCustomerId().orElse(UNRESOLVABLE_CUSTOMER_ID) : requestedCustomerId;
    }

    private boolean isCustomerOnly() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        Set<String> authorities = jwtAuthentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains(CUSTOMER_AUTHORITY)
                && authorities.stream().noneMatch(STAFF_AUTHORITIES::contains);
    }

    private Optional<UUID> ownCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return Optional.empty();
        }
        String customerIdClaim = jwtAuthentication.getToken().getClaimAsString("customer_id");
        if (customerIdClaim == null || customerIdClaim.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(customerIdClaim));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
