package org.vornex.jwtapi;
// JwtEngine.java

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.util.Objects.requireNonNull;


public final class JwtEngineImpl implements JwtEngine {

    private static final Logger log = LoggerFactory.getLogger(JwtEngineImpl.class);

    private static final Set<String> RESERVED = Set.of(
            "iss", "sub", "aud", "exp", "nbf", "iat", "jti", "token_use"
    );

    private final RsaKeyProvider keyProvider;
    private final JwtConfig cfg;
    private final Clock clock;

    // Кэш парсера на текущий публичный ключ (потокобезопасно)
    private volatile JwtParser cachedParser;
    private volatile RSAPublicKey lastKey;
    private final Object parserLock = new Object();

    public JwtEngineImpl(RsaKeyProvider keyProvider, JwtConfig cfg, Clock clock) {
        this.keyProvider = requireNonNull(keyProvider, "keyProvider must not be null");
        this.cfg = requireNonNull(cfg, "cfg must not be null");
        this.clock = (clock == null) ? Clock.systemUTC() : clock;
    }

    /**
     * Генерация токена: единый путь для access/refresh
     */
    public String generateToken(
            String subject,
            Duration validity,
            TokenKind kind,
            Map<String, Object> extraClaims
    ) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (validity == null || validity.isNegative() || validity.isZero()) {
            throw new IllegalArgumentException("validity must be positive");
        }

        final Instant now = clock.instant();
        final Instant exp = now.plus(validity);
        final String jti = UUID.randomUUID().toString();
        // Санитизация пользовательских claims
        Map<String, Object> claims = new LinkedHashMap<>();
        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach((k, v) -> {
                if (!RESERVED.contains(k) && v != null) {
                    claims.put(k, v);
                }
            });
        }

        // Стандартные и обязательные
        claims.put("token_use", kind.asClaimValue());
        claims.put("jti", jti);

        var builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .header().keyId(keyProvider.getActiveKeyId()).and() // kid для ротации
                .signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256);

        if (cfg.checkIssuer()) builder.issuer(cfg.issuer());
        if (cfg.checkAudience()) builder.audience().add(cfg.audience()).and();

        return builder.compact();
    }

    /**
     * Валидация и парсинг с ожиданием конкретного типа токена
     */
    public Claims parseAndValidate(String token, TokenKind expectedKind) {
        String normalized = preValidateStructure(token);
        Jws<Claims> jws;
        try {
            jws = getParser().parseSignedClaims(normalized);
        } catch (SecurityException | MalformedJwtException | IllegalArgumentException e) {
            // Не логируем сам токен
            throw new JwtException("Invalid JWT", e);
        }
        Claims c = jws.getPayload();

        // Дополнительные семантические проверки
        if (c.getSubject() == null || c.getSubject().isBlank()) {
            throw new JwtException("Missing subject");
        }
        String tokenUse = c.get("token_use", String.class);
        if (tokenUse == null) {
            throw new JwtException("Missing token_use");
        }
        TokenKind actual = TokenKind.fromClaimValue(tokenUse);
        if (actual != expectedKind) {
            throw new JwtException("Unexpected token_use: " + tokenUse);
        }
        if (cfg.checkIssuer() && !cfg.issuer().equals(c.getIssuer())) {
            throw new JwtException("Issuer mismatch");
        }
        if (cfg.checkAudience()) {
            var aud = c.getAudience();
            if (aud == null || !aud.contains(cfg.audience())) {
                throw new JwtException("Audience mismatch");
            }
        }
        // exp/nbf/iat проверяются парсером с учётом clockSkew
        return c;
    }

    public Optional<String> extractSubjectSafe(String token, TokenKind kind) {
        try {
            return Optional.ofNullable(parseAndValidate(token, kind).getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    public Optional<Instant> extractExpirationSafe(String token, TokenKind kind) {
        try {
            Date d = parseAndValidate(token, kind).getExpiration();
            return d == null ? Optional.empty() : Optional.of(d.toInstant());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private JwtParser getParser() {
        RSAPublicKey current = keyProvider.getPublicKey();
        JwtParser local = cachedParser;
        if (local != null && current.equals(lastKey)) return local;

        synchronized (parserLock) {
            if (cachedParser == null || !current.equals(lastKey)) {
                cachedParser = Jwts.parser()
                        .verifyWith(current)
                        .clock(() -> Date.from(clock.instant()))
                        .clockSkewSeconds(cfg.clockSkewSeconds())
                        .build();
                lastKey = current;
            }
            return cachedParser;
        }
    }

    private static String preValidateStructure(String token) {
        if (token == null) throw new JwtException("Token is null");
        String t = token.strip();
        // Быстрая защита от мусорных строк (до дорогостоящего парсинга)
        if (!t.matches("^[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+$")) {
            throw new JwtException("Token format is invalid");
        }
        return t;
    }
}
