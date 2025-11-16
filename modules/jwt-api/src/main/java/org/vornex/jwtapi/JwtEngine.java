package org.vornex.jwtapi;

import io.jsonwebtoken.Claims;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface JwtEngine {

    /**
     * Генерация токена: единый путь для access/refresh
     */
    String generateToken(
            String subject,
            Duration validity,
            TokenKind kind,
            Map<String, Object> extraClaims
    );

    /**
     * Валидация и парсинг с ожиданием конкретного типа токена
     */
    Claims parseAndValidate(String token, TokenKind expectedKind);

    /**
     * Извлечение subject из токена без выбрасывания исключений
     */
    Optional<String> extractSubjectSafe(String token, TokenKind kind);

    /**
     * Извлечение времени истечения токена без выбрасывания исключений
     */
    Optional<Instant> extractExpirationSafe(String token, TokenKind kind);
}
