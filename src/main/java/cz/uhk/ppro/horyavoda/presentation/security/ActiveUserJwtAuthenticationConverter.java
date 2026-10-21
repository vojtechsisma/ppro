package cz.uhk.ppro.horyavoda.presentation.security;

import cz.uhk.ppro.horyavoda.application.service.AuthService;
import cz.uhk.ppro.horyavoda.domain.model.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Převod ověřeného JWT na autentizaci: token je platný jen tehdy,
 * pokud jeho uživatel stále existuje a je aktivní. Role se mapuje na autoritu ROLE_ADMIN / ROLE_STAFF.
 */
@Component
public class ActiveUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthService authService;

    public ActiveUserJwtAuthenticationConverter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = authService.findActiveUser(jwt.getSubject())
                .orElseThrow(() -> new InvalidBearerTokenException("Uživatel k tokenu neexistuje nebo je deaktivován."));
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return new JwtAuthenticationToken(jwt, List.of(authority), user.getUsername());
    }
}
