package com.telcocrm.notificationservice.config;

import com.telcocrm.notificationservice.entity.AuditLogEntry;
import com.telcocrm.notificationservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAuditListener {

    private final AuditLogRepository auditLogRepository;

    public void logCreate(String entityType, String entityId, Object newValues) {
        saveAudit(entityType, entityId, "CREATE", null, newValues);
    }

    public void logUpdate(String entityType, String entityId, Object oldValues, Object newValues) {
        saveAudit(entityType, entityId, "UPDATE", oldValues, newValues);
    }

    public void logDelete(String entityType, String entityId, Object oldValues) {
        saveAudit(entityType, entityId, "DELETE", oldValues, null);
    }

    private void saveAudit(String entityType, String entityId, String action, Object oldValues, Object newValues) {
        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .changedBy(getCurrentUser())
                    .changedAt(java.time.LocalDateTime.now())
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    private String getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
