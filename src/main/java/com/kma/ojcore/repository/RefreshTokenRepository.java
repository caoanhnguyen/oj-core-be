package com.kma.ojcore.repository;

import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    void deleteByUser(User user);

    void deleteByToken(String token);
}

