package com.telcocrm.identityservice.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Slf4j
public class KeycloakAdminClientConfig {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLIENT_REGISTRATION_ID = "keycloak-admin";
    private static final String PRINCIPAL = "identity-service";

    @Bean
    public RequestInterceptor keycloakAdminAuthInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return template -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                    .principal(PRINCIPAL)
                    .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient != null) {
                template.header("Authorization", BEARER_PREFIX + authorizedClient.getAccessToken().getTokenValue());
            } else {
                log.warn("Failed to obtain Keycloak admin access token via client credentials");
            }
        };
    }
}
