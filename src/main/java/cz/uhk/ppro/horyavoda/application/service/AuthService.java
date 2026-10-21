package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.domain.exception.AuthenticationFailedException;
import cz.uhk.ppro.horyavoda.domain.exception.EntityAlreadyExistsException;
import cz.uhk.ppro.horyavoda.domain.model.User;
import cz.uhk.ppro.horyavoda.domain.model.UserRole;
import cz.uhk.ppro.horyavoda.infrastructure.persistence.UserRepository;
import cz.uhk.ppro.horyavoda.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Registrace a přihlašování personálu půjčovny. */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Registrace nového uživatele; přihlašovací jméno i e-mail musí být unikátní. */
    public User registerUser(String username, String email, String password, String fullName, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new EntityAlreadyExistsException("Uživatel s přihlašovacím jménem '" + username + "' již existuje.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new EntityAlreadyExistsException("Uživatel s e-mailem '" + email + "' již existuje.");
        }
        User user = new User(username, email, passwordEncoder.encode(password), fullName, role);
        return userRepository.save(user);
    }

    /** Ověření jména a hesla a vystavení JWT access tokenu. */
    @Transactional(readOnly = true)
    public AuthenticatedSession login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getHashedPassword()))
                .orElseThrow(() -> new AuthenticationFailedException("Neplatné uživatelské jméno nebo heslo."));
        if (!user.isActive()) {
            throw new AuthenticationFailedException("Uživatelský účet je deaktivován.");
        }
        return new AuthenticatedSession(user, jwtTokenProvider.createAccessToken(user));
    }

    /** Vyhledá aktivního uživatele podle jména (používá se při ověření tokenu). */
    @Transactional(readOnly = true)
    public Optional<User> findActiveUser(String username) {
        return userRepository.findByUsername(username).filter(User::isActive);
    }
}
