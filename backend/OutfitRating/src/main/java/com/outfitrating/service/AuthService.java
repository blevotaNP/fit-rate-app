package com.outfitrating.service;

import com.outfitrating.domain.RefreshToken;
import com.outfitrating.domain.Role;
import com.outfitrating.domain.User;
import com.outfitrating.dto.LoginRequest;
import com.outfitrating.dto.RegisterRequest;
import com.outfitrating.exception.ConflictException;
import com.outfitrating.repo.RefreshTokenRepository;
import com.outfitrating.repo.UserRepository;
import com.outfitrating.security.JwtService;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, sign-in and refresh-token rotation. */
@Service
public class AuthService {

    /** Returned to the controller, which decides how each half travels to the client. */
    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds, User user) {
    }

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenPair register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("That username is already taken");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that e-mail already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? username
                : request.displayName().trim());
        user.setRole(Role.USER);
        users.save(user);

        return issueTokens(user);
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.login(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Wrong login or password");
        }
        User user = users.findByUsernameIgnoreCase(request.login())
                .or(() -> users.findByEmailIgnoreCase(request.login()))
                .orElseThrow(() -> new BadCredentialsException("Wrong login or password"));
        return issueTokens(user);
    }

    /**
     * Rotates a refresh token: the presented one is revoked and a fresh pair is issued, so a stolen
     * token is usable at most once before the legitimate client invalidates it.
     */
    @Transactional
    public TokenPair refresh(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new BadCredentialsException("No refresh token was presented");
        }
        RefreshToken stored = refreshTokens.findByTokenHash(jwtService.hashRefreshToken(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Unknown refresh token"));
        if (!stored.isActive()) {
            throw new BadCredentialsException("This session has expired, please sign in again");
        }
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(jwtService.hashRefreshToken(presentedToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private TokenPair issueTokens(User user) {
        String rawRefresh = jwtService.generateRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(jwtService.hashRefreshToken(rawRefresh));
        token.setExpiresAt(Instant.now().plus(jwtService.refreshTokenTtl()));
        refreshTokens.save(token);

        return new TokenPair(
                jwtService.issueAccessToken(user),
                rawRefresh,
                jwtService.accessTokenTtl().toSeconds(),
                user);
    }
}
