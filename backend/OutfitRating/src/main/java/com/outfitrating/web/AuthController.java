package com.outfitrating.web;

import com.outfitrating.config.AppProperties;
import com.outfitrating.dto.AuthResponse;
import com.outfitrating.dto.LoginRequest;
import com.outfitrating.dto.RegisterRequest;
import com.outfitrating.dto.UpdateProfileRequest;
import com.outfitrating.dto.UserProfileDto;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.AuthService;
import com.outfitrating.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration, sign-in and session lifecycle.
 *
 * <p>The access token travels in the response body for the SPA to hold in memory; the refresh
 * token travels only in an HttpOnly cookie, so a cross-site script cannot read it.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    static final String REFRESH_COOKIE = "or_refresh";

    private final AuthService authService;
    private final UserService userService;
    private final AppProperties properties;

    public AuthController(AuthService authService, UserService userService, AppProperties properties) {
        this.authService = authService;
        this.userService = userService;
        this.properties = properties;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and start a session")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respondWithSession(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respondWithSession(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh cookie and mint a new access token")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return respondWithSession(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in member")
    public UserProfileDto me(@AuthenticationPrincipal AppUserDetails principal) {
        return userService.profileOf(principal.id());
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the signed-in profile")
    public UserProfileDto updateMe(@AuthenticationPrincipal AppUserDetails principal,
                                   @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.id(), request);
    }

    private ResponseEntity<AuthResponse> respondWithSession(AuthService.TokenPair tokens) {
        AuthResponse body = AuthResponse.of(
                tokens.accessToken(),
                tokens.expiresInSeconds(),
                userService.profileOf(tokens.user().getId()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(String value) {
        return baseCookie(value)
                .maxAge(Duration.ofDays(properties.jwt().refreshTokenDays()))
                .build();
    }

    private ResponseCookie expiredCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.jwt().secureCookie())
                .path("/api/auth")
                .sameSite("Lax");
    }
}
