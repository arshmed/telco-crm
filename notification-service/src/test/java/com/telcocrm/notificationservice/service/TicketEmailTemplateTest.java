package com.telcocrm.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketEmailTemplateTest {

    private final SpringTemplateEngine templateEngine = buildEngine();

    @Test
    void shouldRenderTicketOpenedWithEventPayload() {
        UUID ticketId = UUID.randomUUID();
        Context context = contextOf(Map.of(
                "ticketId", ticketId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "category", "COMPLAINT",
                "priority", "URGENT",
                "assignedTeam", "complaint-team",
                "slaDueAt", "2026-07-16T19:00:00",
                "email", "john@example.com",
                "firstName", "John",
                "lastName", "Doe"));

        String html = templateEngine.process("email/ticket-opened", context);

        assertThat(html).contains(ticketId.toString(), "COMPLAINT", "URGENT",
                "complaint-team", "2026-07-16T19:00:00", "John", "Doe");
    }

    @Test
    void shouldRenderTicketResolvedWithEventPayload() {
        UUID ticketId = UUID.randomUUID();
        Context context = contextOf(Map.of(
                "ticketId", ticketId.toString(),
                "customerId", UUID.randomUUID().toString(),
                "resolution", "Hat arizasi giderildi",
                "resolvedAt", "2026-07-16T18:30:00",
                "email", "john@example.com",
                "firstName", "John",
                "lastName", "Doe"));

        String html = templateEngine.process("email/ticket-resolved", context);

        assertThat(html).contains(ticketId.toString(), "Hat arizasi giderildi",
                "2026-07-16T18:30:00", "John", "Doe");
    }

    private Context contextOf(Map<String, Object> payload) {
        Context context = new Context();
        context.setVariables(payload);
        return context;
    }

    private SpringTemplateEngine buildEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
