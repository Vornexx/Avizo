package org.vornex.auth.config;

// JwtBeansConfig.java

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vornex.auth.repository.RefreshTokenRepository;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.auth.service.RefreshTokenService;
import org.vornex.auth.service.impl.AccessTokenServiceImpl;
import org.vornex.auth.service.impl.RefreshTokenServiceImpl;
import org.vornex.jwtapi.JwtConfig;
import org.vornex.jwtapi.JwtEngineImpl;
import org.vornex.jwtapi.RsaKeyProvider;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class JwtBeansConfig {


    @Bean
    JwtConfig jwtConfig(
            // допустим, из application.yml
            @Value("${jwt.issuer:}") String issuer,
            @Value("${jwt.audience:}") String audience,
            @Value("${jwt.clock-skew-seconds:60}") long skew
    ) {
        return new JwtConfig(issuer, audience, skew);
    }

    @Bean
    JwtEngineImpl jwtEngine(RsaKeyProvider kp, JwtConfig cfg, Clock clock) {
        return new JwtEngineImpl(kp, cfg, clock);
    }

    @Bean
    AccessTokenService accessTokenService(
            JwtEngineImpl engine,
            @Value("${jwt.access.ttl.seconds:900}") long accessTtlSeconds
    ) {
        return new AccessTokenServiceImpl(engine, Duration.ofSeconds(accessTtlSeconds));
    }

    @Bean
    RefreshTokenService refreshTokenService(
            JwtEngineImpl engine,
            RefreshTokenRepository repo,
            Clock clock,
            @Value("${jwt.refresh.ttl.seconds:604800}") long refreshTtlSeconds,
            @Value("${jwt.refresh.hmac.pepper}") String pepper
    ) {
        return new RefreshTokenServiceImpl(
                engine, repo, Duration.ofSeconds(refreshTtlSeconds), clock, pepper
        );
    }
}
