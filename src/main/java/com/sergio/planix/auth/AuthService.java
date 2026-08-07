package com.sergio.planix.auth;

import com.sergio.planix.auth.dto.*;
import com.sergio.planix.common.EmailAlreadyUsedException;
import com.sergio.planix.common.InvalidCredentialsException;
import com.sergio.planix.common.InvalidRefreshTokenException;
import com.sergio.planix.common.NotFoundException;
import com.sergio.planix.common.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final Duration refreshTtl;
    private final String dummyHash;

    public AuthService(UserRepository userRepo, RefreshTokenRepository refreshRepo,
                       PasswordEncoder encoder, JwtService jwtService, CurrentUser currentUser,
                       @Value("${planix.jwt.refresh-ttl}") Duration refreshTtl) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.refreshTtl = refreshTtl;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse register(RegisterRequest req) {
        String email = Emails.normalize(req.email());
        if (userRepo.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("Já existe uma conta com o e-mail " + email);
        }
        User user = userRepo.save(new User(req.name(), email, encoder.encode(req.password())));
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        Optional<User> found = userRepo.findByEmail(Emails.normalize(req.email()));

        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean senhaConfere = encoder.matches(req.password(), hash);

        if (found.isEmpty() || !senhaConfere) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }
        return issueTokens(found.get());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshRepo.findByTokenHash(Tokens.sha256(presentedToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token inválido ou expirado"));

        if (stored.isRevoked()) {
            refreshRepo.revokeAllOf(stored.getUser().getId(), OffsetDateTime.now());
            throw new InvalidRefreshTokenException("Refresh token inválido ou expirado");
        }
        if (stored.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token inválido ou expirado");
        }

        stored.setRevokedAt(OffsetDateTime.now());
        return issueTokens(stored.getUser());
    }

    public void logout(String presentedToken) {
        refreshRepo.findByTokenHash(Tokens.sha256(presentedToken))
                .ifPresent(rt -> rt.setRevokedAt(OffsetDateTime.now()));
    }

    public void logoutAll() {
        refreshRepo.revokeAllOf(currentUser.id(), OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return userRepo.findById(currentUser.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    AuthResponse rotateSessions(User user) {
        refreshRepo.revokeAllOf(user.getId(), OffsetDateTime.now());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String refreshValue = Tokens.random();

        refreshRepo.save(new RefreshToken(
                user,
                Tokens.sha256(refreshValue),
                OffsetDateTime.now().plus(refreshTtl)));

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshValue,
                jwtService.accessTtl().toSeconds());
    }
}
