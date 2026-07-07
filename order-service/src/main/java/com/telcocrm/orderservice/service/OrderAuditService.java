package com.telcocrm.orderservice.service;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderAuditLog;
import com.telcocrm.orderservice.repository.OrderAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            return authentication.getName();
        }
        return SYSTEM_ACTOR;
    }
}
