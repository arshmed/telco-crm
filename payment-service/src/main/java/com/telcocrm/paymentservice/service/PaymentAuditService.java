package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.entity.Payment;
import com.telcocrm.paymentservice.entity.PaymentAuditLog;
import com.telcocrm.paymentservice.repository.PaymentAuditLogRepository;
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
public class PaymentAuditService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final PaymentAuditLogRepository paymentAuditLogRepository;

    public void log(Payment payment, String detail) {
        PaymentAuditLog entry = PaymentAuditLog.builder()
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus())
                .detail(detail)
                .performedBy(resolvePerformedBy())
                .build();

        paymentAuditLogRepository.save(entry);
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
