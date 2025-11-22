package org.vornex.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.auth.dto.TokenPair;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;
import org.vornex.auth.service.AuthService;
import org.vornex.auth.util.RefreshTokenExtractor;
import org.vornex.authapi.AuthUserData;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Clock clock;
    private final String cookieAccessName;
    private final String cookieRefreshName;
    private final RefreshTokenExtractor refreshTokenExtractor;

    public AuthController(AuthService authService,
                          Clock clock,
                          @Value("${security.auth.access-cookie-name}") String cookieAccessName,
                          @Value("${security.auth.refresh-cookie-name}") String cookieRefreshName, RefreshTokenExtractor refreshTokenExtractor) {
        this.authService = authService;
        this.clock = clock;
        this.cookieAccessName = cookieAccessName;
        this.cookieRefreshName = cookieRefreshName;
        this.refreshTokenExtractor = refreshTokenExtractor;
    }


    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid UserLoginDto loginDto, HttpServletResponse response) {
        try {
            TokenPair tokens = authService.login(loginDto);

            createTokenCookies(tokens, clock).forEach(cookie ->
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
            );

            return ResponseEntity.noContent().build();
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid UserRegistrationDto registrationDto, HttpServletResponse response) {
        TokenPair tokens = authService.register(registrationDto);           // создаём user// получаем токены + expiry

        createTokenCookies(tokens, clock).forEach(cookie ->
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthUserData userData) {
        authService.logout(userData.getId().toString());
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenExtractor.extract(request); // один вызов
        TokenPair tokens = authService.refreshTokens(refreshToken);

        createTokenCookies(tokens, clock)
                .forEach(cookie -> response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString()));

        return ResponseEntity.noContent().build();
    }


    private ResponseCookie createHttpOnlyCookie(String name, String value, Instant expiry, Clock clock) {
        if (expiry == null) {
            // session cookie
            return ResponseCookie.from(name, value)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Lax")
                    .build();
        }
        long seconds = Duration.between(clock.instant(), expiry).getSeconds();
        if (seconds <= 0) {
            // уже истек — не ставим куку
            throw new IllegalStateException("Token already expired");
        }
        // clamp to Integer.MAX_VALUE seconds (safety)
        long clamped = Math.min(seconds, Integer.MAX_VALUE);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax") // Lax обычно удобнее для логина, Strict — безопаснее
                .maxAge(Duration.ofSeconds(clamped))
                .build();
    }

    private List<ResponseCookie> createTokenCookies(TokenPair tokens, Clock clock) {
        List<ResponseCookie> cookies = new ArrayList<>();
        // access
        cookies.add(createHttpOnlyCookie(cookieAccessName, tokens.accessToken(), tokens.accessExpiry(), clock));
        // refresh
        cookies.add(createHttpOnlyCookie(cookieRefreshName, tokens.refreshToken(), tokens.refreshExpiry(), clock));
        return cookies;
    }

}
