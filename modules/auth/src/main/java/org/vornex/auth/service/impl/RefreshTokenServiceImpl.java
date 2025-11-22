package org.vornex.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.vornex.auth.entity.RefreshTokenEntity;
import org.vornex.auth.repository.RefreshTokenRepository;
import org.vornex.auth.service.RefreshTokenService;
import org.vornex.jwtapi.JwtEngineImpl;
import org.vornex.jwtapi.TokenKind;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtEngineImpl engine;
    private final RefreshTokenRepository repo;
    private final Duration validity;
    private final Clock clock;

    // «Перец» для HMAC – НЕ хранить в БД, только в конфиге/менеджере секретов
    private final byte[] pepper;

    public RefreshTokenServiceImpl(JwtEngineImpl engine,
                                   RefreshTokenRepository repo,
                                   Duration validity /* e.g. 7d */,
                                   Clock clock,
                                   String pepperSecret /* strong random base64/string */) {
        this.engine = requireNonNull(engine, "engine must not be null");
        this.repo = requireNonNull(repo, "repo must not be null");
        if (validity == null || validity.isNegative() || validity.isZero())
            throw new IllegalArgumentException("validity must be positive");
        this.validity = validity;
        this.clock = (clock == null) ? Clock.systemUTC() : clock;
        if (pepperSecret == null || pepperSecret.isBlank())
            throw new IllegalArgumentException("pepperSecret must be set");
        this.pepper = pepperSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String create(String subject, Map<String, Object> claims) {
        // Генерируем refresh через общее ядро
        String token = engine.generateToken(subject, validity, TokenKind.REFRESH, claims);
        Claims c = engine.parseAndValidate(token, TokenKind.REFRESH);

        // Сохраняем по HMAC(token) и jti
        String tokenHash = hmacSha256Base64(token);
        String jti = c.getId(); // в JJWT jti хранится в claim "jti" и доступен как getId()
        if (jti == null || jti.isBlank()) {
            // fallback: берем из claims напрямую
            jti = Objects.toString(c.get("jti"), null);
        }
        if (jti == null || jti.isBlank()) {
            throw new IllegalStateException("Generated refresh token must contain jti");
        }
        Instant iat = c.getIssuedAt().toInstant();
        Instant exp = c.getExpiration().toInstant();

        repo.save(new RefreshTokenEntity(subject, tokenHash, jti, iat, exp));
        return token;
    }

    private RefreshTokenEntity verifyRefreshToken(String token) {
        Claims c = engine.parseAndValidate(token, TokenKind.REFRESH);
        String jti = c.getId();
        if (jti == null) jti = Objects.toString(c.get("jti"), null);

        RefreshTokenEntity e = repo.findByJti(jti)
                .orElseThrow(() -> new JwtException("Refresh token not found"));
        if (e.isRevoked() || e.getExpiresAt().isBefore(clock.instant()))
            throw new JwtException("Refresh token expired or revoked");

        // Доп. сверка хэша
        String expectedHash = e.getTokenHash();
        if (!Objects.equals(expectedHash, hmacSha256Base64(token))) {
            throw new JwtException("Token does not match stored record");
        }
        return e;
    }

    @Override
    @Transactional
    public Claims consume(String token) {
        RefreshTokenEntity e = verifyRefreshToken(token);
        e.setRevoked(true); // одноразовый
        // @Version обеспечит оптимистическую блокировку: параллельное consume вторым потоком упадет
        // после commit выбросит OptimisticLockException либо будет повторно прочитан уже revoked
        return engine.parseAndValidate(token, TokenKind.REFRESH);
    }

    @Override
    public Claims validate(String token) {
        verifyRefreshToken(token);
        return engine.parseAndValidate(token, TokenKind.REFRESH);
    }

    @Override
    public void revoke(String token) {
        // Безопаснее идти через parse -> jti
        Claims c = engine.parseAndValidate(token, TokenKind.REFRESH);
        String jti = c.getId();
        if (jti == null) jti = Objects.toString(c.get("jti"), null);

        repo.findByJti(jti).ifPresent(e -> {
            if (!e.isRevoked()) {
                e.setRevoked(true);
                e.setRevokedAt(Instant.now());
                repo.save(e);
            }
        });
    }

    @Override
    @Transactional
    public void revokeByUser(String userId, Instant now) {
        repo.revokeByUserId(userId, now);
    }

    @Override
    public Optional<String> extractSubject(String token) {
        return engine.extractSubjectSafe(token, TokenKind.REFRESH);
    }

    @Override
    public Optional<Instant> extractExpiration(String token) {
        return engine.extractExpirationSafe(token, TokenKind.REFRESH);
    }

    private String hmacSha256Base64(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            byte[] out = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
