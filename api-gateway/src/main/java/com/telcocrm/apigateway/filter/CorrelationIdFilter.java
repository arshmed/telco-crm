package com.telcocrm.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    static final String CORRELATION_ID_HEADER = "Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getHeaders().get(CORRELATION_ID_HEADER) != null) {
            return chain.filter(exchange);
        }

        String correlationId = UUID.randomUUID().toString();
        ServerWebExchange mutated = exchange.mutate()
            .request(r -> r.header(CORRELATION_ID_HEADER, correlationId))
            .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -2; // runs before JwtHeaderRelayFilter
    }
}
