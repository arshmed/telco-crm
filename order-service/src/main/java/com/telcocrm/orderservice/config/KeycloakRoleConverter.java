package com.telcocrm.orderservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof List<?> roleList) {
                roleList.stream()
                        .filter(String.class::isInstance)
                        .map(r -> new SimpleGrantedAuthority((String) r))
                        .forEach(authorities::add);
            }
        }

        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        authorities.addAll(scopeConverter.convert(jwt));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
