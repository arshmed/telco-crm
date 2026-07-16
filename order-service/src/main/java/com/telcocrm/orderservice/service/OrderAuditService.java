package com.telcocrm.orderservice.service;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderAuditLog;
import com.telcocrm.orderservice.repository.OrderAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAuditService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final OrderAuditLogRepository orderAuditLogRepository;

    public void log(Order order, String detail) {
        OrderAuditLog entry = OrderAuditLog.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus())
                .sagaStep(order.getSagaState().getCurrentStep())
                .detail(detail)
                .performedBy(resolvePerformedBy())
                .build();

        orderAuditLogRepository.save(entry);
    }

    private String resolvePerformedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                String preferredUsername = jwtAuthentication.getToken().getClaimAsString("preferred_username");
                if (preferredUsername != null && !preferredUsername.isBlank()) {
                    return preferredUsername;
                }
            }

            String name = authentication.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return SYSTEM_ACTOR;
    }
}
