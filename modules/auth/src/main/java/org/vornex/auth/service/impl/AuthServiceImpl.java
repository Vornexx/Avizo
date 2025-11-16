package org.vornex.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.auth.dto.TokenPair;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;
import org.vornex.auth.mapper.AdapterUserMapper;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.auth.service.AuthService;
import org.vornex.auth.service.RefreshTokenService;
import org.vornex.userapi.UserAccountDto;
import org.vornex.userapi.UserManagementPort;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdapterUserMapper mapper;
    private final UserManagementPort userService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final List<String> DEFAULT_ROLES = List.of("ROLE_USER");


    @Override
    public TokenPair login(UserLoginDto loginDto) {
        normalizeLoginDto(loginDto);
        UserAccountDto accountDto = mapper.toUserAccountDto(loginDto);

        UserAccountDto fullUserData = userService.findByEmailOrNumber(accountDto)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!encoder.matches(accountDto.password(), fullUserData.password())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return generateTokens(fullUserData);
    }

    @Override
    @Transactional // Создание токенов тоже транзакции. Если они упадут пользователь уже будет создан без токенов
    public TokenPair register(UserRegistrationDto regDto) {

        normalizeRegistrationDto(regDto);

        if (userService.existsByUsername(regDto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (regDto.getEmail() != null && userService.existsByEmail(regDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String hashPassword = encoder.encode(regDto.getPassword());
        UserAccountDto toCreate = mapper.toUserAccountDto(regDto, hashPassword);

        try {
            UserAccountDto created = userService.create(toCreate);
            // Optionally set status to PENDING and send verification email here
            return generateTokens(created);
        } catch (DataIntegrityViolationException ex) { // добавить unique constraint на email+username.
            log.warn("Unique constraint violated during registration for username={} email={}", regDto.getUsername(), maskEmail(regDto.getEmail()));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        } catch (Exception ex) {
            log.error("Unexpected error during registration", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    public TokenPair generateTokens(UserAccountDto user) {
        Map<String, Object> claims = Map.of(
                "username", user.username(),
                "roles", DEFAULT_ROLES,
                "locale", "ru" //убрали email и phone.
        );

        String accessToken = accessTokenService.generateAccessToken(user.id().toString(), claims);
        String refreshToken = refreshTokenService.create(user.id().toString(), Collections.emptyMap());

        // Получаем expiry через jwt-api (engine)
        Instant accessExp = accessTokenService.extractExpiration(accessToken)
                .orElseThrow(() -> new IllegalStateException("Access token has no exp"));
        Instant refreshExp = refreshTokenService.extractExpiration(refreshToken)
                .orElseThrow(() -> new IllegalStateException("Refresh token has no exp"));

        return new TokenPair(accessToken, refreshToken, accessExp, refreshExp);
    }

    public TokenPair refreshTokens(String refreshToken) {
        // 1. consume -> проверка валидности и одноразовость
        Claims claims = refreshTokenService.consume(refreshToken);
        String userId = claims.getSubject();
        if (userId == null) throw new JwtException("Refresh token has no subject");

        // 2. ищем пользователя (если нужно)
        var user = userService.findById(UUID.fromString(userId));

        // 3. генерируем новую пару токенов
        Map<String, Object> tokenClaims = Map.of(
                "username", user.username(),
                "roles", List.of("ROLE_USER")
        );

        String newAccess = accessTokenService.generateAccessToken(userId, tokenClaims);
        String newRefresh = refreshTokenService.create(userId, Map.of());

        Instant accessExp = accessTokenService.extractExpiration(newAccess)
                .orElseThrow();
        Instant refreshExp = refreshTokenService.extractExpiration(newRefresh)
                .orElseThrow();

        return new TokenPair(newAccess, newRefresh, accessExp, refreshExp);
    }


    public void normalizeRegistrationDto(UserRegistrationDto regDto) {
        regDto.setEmail(Optional.ofNullable(regDto.getEmail()).map(String::trim).map(String::toLowerCase).orElse(null));
        regDto.setPhoneNumber(Optional.ofNullable(regDto.getPhoneNumber()).map(String::trim).orElse(null));
        regDto.setUsername(Optional.ofNullable(regDto.getUsername()).map(String::trim).orElse(null));
        regDto.setFirstName(Optional.ofNullable(regDto.getFirstName()).map(String::trim).orElse(null));
        regDto.setLastName(Optional.ofNullable(regDto.getLastName()).map(String::trim).orElse(null));
    }
    private void normalizeLoginDto(UserLoginDto loginDto) {
        loginDto.setEmail(Optional.ofNullable(loginDto.getEmail())
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse(null));
        loginDto.setPhoneNumber(Optional.ofNullable(loginDto.getPhoneNumber())
                .map(String::trim)
                .orElse(null));
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***@***";
        return email.charAt(0) + "***@" + email.substring(at + 1);
    }


    @Override
    public void logout(String userId) {
        Instant now = Instant.now(clock);
        refreshTokenService.revokeByUser(userId, now);
    }
}
