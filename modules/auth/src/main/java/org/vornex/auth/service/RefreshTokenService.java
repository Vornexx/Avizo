package org.vornex.auth.service;

import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface RefreshTokenService {
    String create(String subject, Map<String, Object> claims);

    /**
     * Одноразовое «поглощение»: валидирует и помечает токен revoked=true
     */
    Claims consume(String token);

    /**
     * Проверка без изменения состояния
     */
    Claims validate(String token);

    void revoke(String token);

    void revokeByUser(String userId, Instant now);

    Optional<String> extractSubject(String token);

    Optional<Instant> extractExpiration(String token);
}
