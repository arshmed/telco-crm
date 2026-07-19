package com.telcocrm.bff_server.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

final class SessionSynchronizedAuthorizedClientManager implements OAuth2AuthorizedClientManager {

    private final OAuth2AuthorizedClientManager delegate;
    private final ConcurrentHashMap<String, Lock> sessionLocks = new ConcurrentHashMap<>();

    SessionSynchronizedAuthorizedClientManager(OAuth2AuthorizedClientManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizeRequest authorizeRequest) {
        Lock lock = sessionLocks.computeIfAbsent(resolveLockKey(authorizeRequest), key -> new ReentrantLock());
        lock.lock();
        try {
            return delegate.authorize(authorizeRequest);
        } finally {
            lock.unlock();
        }
    }

    private String resolveLockKey(OAuth2AuthorizeRequest authorizeRequest) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpSession session = servletRequestAttributes.getRequest().getSession(false);
            if (session != null) {
                return session.getId();
            }
        }
        return authorizeRequest.getClientRegistrationId() + ":" + authorizeRequest.getPrincipal().getName();
    }
}
