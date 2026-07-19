package com.telcocrm.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

@Component
public class RateLimitResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                if (HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode()) && !isCommitted()) {
                    return writeRateLimitBody();
                }
                return super.setComplete();
            }

            private Mono<Void> writeRateLimitBody() {
                originalResponse.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                String body = """
                        {"type":"https://telcocrm.com/errors/rate-limit-exceeded","title":"Too Many Requests","status":429,"detail":"İstek limitini aştınız, lütfen bir süre sonra tekrar deneyin."}""";
                DataBuffer buffer = originalResponse.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return originalResponse.writeWith(Mono.just(buffer));
            }
        };
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
