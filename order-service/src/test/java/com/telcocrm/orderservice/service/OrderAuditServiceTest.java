package com.telcocrm.orderservice.service;

import com.telcocrm.orderservice.entity.Order;
import com.telcocrm.orderservice.entity.OrderAuditLog;
import com.telcocrm.orderservice.entity.SagaState;
import com.telcocrm.orderservice.entity.enums.OrderStatus;
import com.telcocrm.orderservice.entity.enums.SagaStep;
import com.telcocrm.orderservice.repository.OrderAuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderAuditServiceTest {

    @Mock
    private OrderAuditLogRepository orderAuditLogRepository;

    @InjectMocks
    private OrderAuditService orderAuditService;

    @Captor
    private ArgumentCaptor<OrderAuditLog> auditLogCaptor;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Order anOrder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PAID)
                .sagaState(SagaState.builder().currentStep(SagaStep.AWAITING_SUBSCRIPTION).build())
                .build();
    }

    @Test
    void shouldRecordPerformedByFromAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("keycloak-user-id", null, List.of()));
        var order = anOrder();

        orderAuditService.log(order, "Payment completed: paymentId=abc");

        verify(orderAuditLogRepository).save(auditLogCaptor.capture());
        OrderAuditLog saved = auditLogCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(order.getId());
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(saved.getSagaStep()).isEqualTo(SagaStep.AWAITING_SUBSCRIPTION);
        assertThat(saved.getDetail()).isEqualTo("Payment completed: paymentId=abc");
        assertThat(saved.getPerformedBy()).isEqualTo("keycloak-user-id");
    }

    @Test
    void shouldFallBackToSystemWhenNoAuthentication() {
        var order = anOrder();

        orderAuditService.log(order, "Order created");

        verify(orderAuditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void shouldFallBackToSystemWhenAuthenticationIsAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        var order = anOrder();

        orderAuditService.log(order, "Order created");

        verify(orderAuditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getPerformedBy()).isEqualTo("SYSTEM");
    }
}
