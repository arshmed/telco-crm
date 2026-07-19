package com.telcocrm.apigateway.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.InetSocketAddress;

@Slf4j
@Configuration
public class RateLimiterConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(extractUserKey(exchange))
            .switchIfEmpty(Mono.fromSupplier(() -> resolveClientIp(exchange)));
    }

    private String extractUserKey(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();
            String subject = claims.getSubject();
            if (StringUtils.hasText(subject)) {
                return subject;
            }
            return claims.getStringClaim("preferred_username");
        } catch (Exception ex) {
            log.debug("Rate limiter could not parse JWT for key resolution, falling back to IP: {}", ex.getMessage());
            return null;
        }
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }
}
