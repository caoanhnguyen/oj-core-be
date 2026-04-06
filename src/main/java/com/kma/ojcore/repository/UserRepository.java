package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.users.UserRankSdo;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.Provider;
import com.kma.ojcore.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.status = 'ACTIVE'")
    Optional<User> findByUserIdAndStatusIsActive(UUID userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findUserWithRolesById(UUID id);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail)")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    /**
     * Get ACM ranking: chỉ lấy user có ROLE_USER, loại ADMIN và MODERATOR,
     * và chỉ lấy những user có solvedCount > 0.
     */
    @Query("SELECT new com.kma.ojcore.dto.response.users.UserRankSdo(" +
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
    @Query("SELECT new com.kma.ojcore.dto.response.users.UserRankSdo(" +
            "u.id, u.username, u.avatarUrl, u.acCount, u.solvedCount, u.submissionCount, u.totalScore) " +
            "FROM User u " +
            "JOIN u.roles r " +
            "WHERE r.name = 'ROLE_USER' " +
            "AND NOT EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR')) " +
            "ORDER BY u.totalScore DESC, u.solvedCount DESC, u.submissionCount ASC")
    Page<UserRankSdo> getOIRanking(Pageable pageable);

    /**
     * Để tránh N+1 khi query phân trang với quan hệ Many to Many, thực hiện 2 query: 1 query lấy user với phân trang, 1 query lấy role của những user đó.
     * @param keyword
     * @param pageable
     * @return
     */
    // Query lấy user với phân trang và filter theo keyword (username, email, fullName) và trạng thái khóa tài khoản
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.roles r " +
            "WHERE (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
            "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:isLocked IS NULL OR u.accountNonLocked = :isLocked) " +
            "AND (:roleName IS NULL OR r.name = :roleName)")
    Page<User> searchUsersForAdmin(@Param("keyword") String keyword,
                                   @Param("isLocked") Boolean isLocked,
                                   @Param("roleName") RoleName roleName,
                                   Pageable pageable);

    // Query 2: Lấy User kèm Role bằng mệnh đề IN (Chống N+1)
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles " +
            "WHERE u.id IN :userIds")
    List<User> findUsersWithRolesByIds(@Param("userIds") List<UUID> userIds);


    // Bulk update accountNonLocked
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :accountNonLocked WHERE u.id IN :userIds")
    void bulkUpdateAccountNonLocked(@Param("accountNonLocked") boolean accountNonLocked,
                                    @Param("userIds") List<UUID> userIds);

    @Modifying
    @Query(value = "UPDATE users u " +
            "SET `total-score` = (SELECT COALESCE(SUM(max_score), 0) FROM user_problem_status WHERE user_id = u.id), " +
            "solved_count = (SELECT COUNT(*) FROM user_problem_status WHERE user_id = u.id AND state = 'SOLVED'), " +
            "ac_count = (SELECT COUNT(*) FROM submissions WHERE user_id = u.id AND verdict = 'AC' AND status = 'ACTIVE') " +
            "WHERE u.id = :userId", nativeQuery = true)
    int recalculateUserStats(@Param("userId") UUID userId);
}
