package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.UserRankSdo;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findUserWithRolesById(UUID id);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    @Query("SELECT u FROM User u WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail)")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    /**
     * Get ACM ranking: chỉ lấy user có ROLE_USER, loại ADMIN và MODERATOR,
     * và chỉ lấy những user có solvedCount > 0.
     */
    @Query("SELECT new com.kma.ojcore.dto.response.UserRankSdo(" +
            "u.id, u.username, u.avatarUrl, u.acCount, u.solvedCount, u.submissionCount, u.totalScore) " +
            "FROM User u " +
            "JOIN u.roles r " +
            "WHERE r.name = 'ROLE_USER' " +
            "AND NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR')) " +
            "ORDER BY u.solvedCount DESC, u.submissionCount ASC, u.acCount DESC")
    Page<UserRankSdo> getACMRanking(Pageable pageable);

    /**
     * Get OI ranking: tương tự, chỉ lấy user có ROLE_USER,
     * và chỉ lấy những user có totalScore > 0 (tuỳ logic bạn có thể bỏ điều kiện này).
     */
    @Query("SELECT new com.kma.ojcore.dto.response.UserRankSdo(" +
            "u.id, u.username, u.avatarUrl, u.acCount, u.solvedCount, u.submissionCount, u.totalScore) " +
            "FROM User u " +
            "JOIN u.roles r " +
            "WHERE r.name = 'ROLE_USER' " +
            "AND NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR')) " +
            "ORDER BY u.totalScore DESC, u.solvedCount DESC, u.submissionCount ASC")
    Page<UserRankSdo> getOIRanking(Pageable pageable);
}
