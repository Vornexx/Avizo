package org.vornex.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vornex.auth.entity.RefreshTokenEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    Optional<RefreshTokenEntity> findByJti(String jti);

    void deleteByExpiresAtBefore(Instant cutoff);

    @Modifying //даем jpa понять что это не select и он вернет кол-во обновленных строк если int, а не void
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true, t.revokedAt = :now WHERE t.userId = :userId")
    void revokeByUserId(@Param("userId") String userId, @Param("now") Instant now);



}
