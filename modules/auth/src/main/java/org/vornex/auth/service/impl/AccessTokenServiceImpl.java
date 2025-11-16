package org.vornex.auth.service.impl;

// AccessTokenServiceImpl.java

import io.jsonwebtoken.Claims;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.jwtapi.JwtEngineImpl;
import org.vornex.jwtapi.TokenKind;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;


public class AccessTokenServiceImpl implements AccessTokenService {

    private final JwtEngineImpl engine;
    private final Duration validity;

    public AccessTokenServiceImpl(JwtEngineImpl engine, Duration validity /* e.g. PT15M */) {
        this.engine = requireNonNull(engine, "engine must not be null");
        if (validity == null || validity.isNegative() || validity.isZero())
            throw new IllegalArgumentException("validity must be positive");
        this.validity = validity;
    }

    @Override
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return engine.generateToken(subject, validity, TokenKind.ACCESS, claims);
    }

    @Override
    public Claims validate(String token) {
        return engine.parseAndValidate(token, TokenKind.ACCESS);
    }

    @Override
    public boolean isValid(String token) {
        try {
            validate(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<String> extractSubject(String token) {
        return engine.extractSubjectSafe(token, TokenKind.ACCESS);
    }

    @Override
    public Optional<Instant> extractExpiration(String token) {
        return engine.extractExpirationSafe(token, TokenKind.ACCESS);
    }
}
