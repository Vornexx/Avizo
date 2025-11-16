package org.vornex.auth.service;

import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

// AccessTokenService.java
public interface AccessTokenService {
    String generateAccessToken(String subject, Map<String, Object> claims);

    Claims validate(String token);            // кидаем JwtException если не валиден

    boolean isValid(String token);            // безопасная проверка

    Optional<String> extractSubject(String token);

    Optional<Instant> extractExpiration(String token);
}
