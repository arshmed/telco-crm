package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.entity.IdentityAuditLog;
import com.telcocrm.identityservice.repository.IdentityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityAuditService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final IdentityAuditLogRepository identityAuditLogRepository;

    public void log(String entityType, UUID entityId, String action, String detail) {
        IdentityAuditLog entry = IdentityAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .detail(detail)
                .performedBy(resolvePerformedBy())
                .build();

        identityAuditLogRepository.save(entry);
    }

    public String resolvePerformedBy() {
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
