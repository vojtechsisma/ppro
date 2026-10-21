package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.AuthService;
import cz.uhk.ppro.horyavoda.application.service.AuthenticatedSession;
import cz.uhk.ppro.horyavoda.domain.exception.AuthenticationFailedException;
import cz.uhk.ppro.horyavoda.presentation.dto.TokenResponse;
import cz.uhk.ppro.horyavoda.presentation.dto.UserRegisterRequest;
import cz.uhk.ppro.horyavoda.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Registrace nového uživatele (personál půjčovny). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return UserResponse.from(authService.registerUser(
                request.username(), request.email(), request.password(), request.fullName(), request.roleOrDefault()));
    }

    /** Přihlášení (formulář username + password) a získání JWT Bearer tokenu. */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse login(@RequestParam String username, @RequestParam String password) {
        AuthenticatedSession session = authService.login(username, password);
        return TokenResponse.bearer(session.accessToken(), UserResponse.from(session.user()));
    }

    /** Profil aktuálně přihlášeného uživatele. */
    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return authService.findActiveUser(jwt.getSubject())
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthenticationFailedException("Uživatel k tokenu neexistuje nebo je deaktivován."));
    }
}
