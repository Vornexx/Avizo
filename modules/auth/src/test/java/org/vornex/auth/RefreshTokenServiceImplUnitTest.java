package org.vornex.auth;


import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.vornex.auth.entity.RefreshTokenEntity;
import org.vornex.auth.repository.RefreshTokenRepository;
import org.vornex.auth.service.RefreshTokenService;
import org.vornex.auth.service.impl.RefreshTokenServiceImpl;
import org.vornex.jwtapi.JwtEngineImpl;
import org.vornex.jwtapi.TokenKind;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenServiceImpl.
 * <p>
 * Мы проверим, что при create() сохраняется запись с HMAC-SHA256(token) и jti.
 */
class RefreshTokenServiceImplUnitTest {

    @Mock
    private JwtEngineImpl engine;
    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService service;
    private final Clock clock = Clock.fixed(Instant.parse("2025-09-01T10:00:00Z"), ZoneOffset.UTC);
    private final String pepper = "super-secret-pepper";

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        service = new RefreshTokenServiceImpl(engine, repo, Duration.ofDays(7), clock, pepper);
    }

    @Test
    void create_savesEntityWithHmacHash() {
        String subject = "user-uuid";
        String generatedToken = "token-value";
        String jti = "jti-123";

        // stub engine.generateToken
        when(engine.generateToken(eq(subject), any(), any(), anyMap())).thenReturn(generatedToken);

        // construct Claims mock returned by parseAndValidate
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(jti);
        when(claims.getIssuedAt()).thenReturn(Date.from(clock.instant()));
        when(claims.getExpiration()).thenReturn(Date.from(clock.instant().plusSeconds(3600)));
        when(engine.parseAndValidate(generatedToken, TokenKind.REFRESH)).thenReturn(claims);

        // Act
        String returned = service.create(subject, Map.of());

        // Assert returned token is the same
        assertThat(returned).isEqualTo(generatedToken);

        // Verify repo.save called and tokenHash equals expected HMAC
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(repo).save(captor.capture());

        RefreshTokenEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(subject);
        assertThat(saved.getJti()).isEqualTo(jti);

        // compute expected HMAC using same algorithm as service
        String expectedHash = computeHmacBase64(generatedToken, pepper);
        assertThat(saved.getTokenHash()).isEqualTo(expectedHash);
    }

    // helper: compute HMAC-SHA256 Base64 exactly like service does
    private static String computeHmacBase64(String token, String pepper) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void validate_whenHashMismatch_throwsJwtException() {
        String token = "token";
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("jti");
        when(engine.parseAndValidate(token, TokenKind.REFRESH)).thenReturn(claims);

        // repository returns an entity with different hash
        RefreshTokenEntity e = new RefreshTokenEntity("user", "different-hash", "jti", clock.instant(), clock.instant().plusSeconds(1000));
        when(repo.findByJti("jti")).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.validate(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
