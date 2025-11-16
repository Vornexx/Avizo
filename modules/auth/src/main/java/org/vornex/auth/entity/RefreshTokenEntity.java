package org.vornex.auth.entity;

// RefreshTokenEntity.java
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_hash", columnList = "tokenHash"),
                @Index(name = "idx_refresh_jti", columnList = "jti", unique = true)
        })
@Getter
public class RefreshTokenEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;     // HMAC-SHA256(token), Base64

    @Column(nullable = false, unique = true, length = 40)
    private String jti;

    @Column(nullable = false)
    private Instant issuedAt;


    @Setter
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = true)
    private Instant revokedAt;

    // getters/setters...
    @Setter
    @Column(nullable = false)
    private boolean revoked = false;

    @Version
    private long version;

    protected RefreshTokenEntity() {}

    public RefreshTokenEntity(String userId, String tokenHash, String jti, Instant iat, Instant exp) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.jti = jti;
        this.issuedAt = iat;
        this.expiresAt = exp;
    }

}
