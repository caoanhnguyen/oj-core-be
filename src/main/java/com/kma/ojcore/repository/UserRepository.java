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

    interface UserRankingProjection {
        byte[] getUserId();
        String getUsername();
        String getAvatarUrl();
        Integer getAcCount();
        Integer getSolvedCount();
        Integer getSubmissionCount();
        Double getTotalScore();
        Integer getRank();
    }

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
     * 2 query lấy ranking cho OI và ACM
     * Cách xếp hạng:
     * - OI: Tổng điểm -> Số bài giải được -> Số AC -> Số submission
     * - ACM: Số bài giải được -> Tổng điểm -> Số AC -> Số submission
     */
    @Query(value = "SELECT " +
            "u.id AS userId, u.username AS username, u.avatar_url AS avatarUrl, " +
            "u.`total-score` AS totalScore, u.solved_count AS solvedCount, u.ac_count AS acCount, u.submission_count AS submissionCount, " +
            "RANK() OVER (ORDER BY u.`total-score` DESC, u.solved_count DESC, u.ac_count DESC, u.submission_count ASC) AS `rank` " +
            "FROM users u " +
            "WHERE u.status = 'ACTIVE' " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM user_roles ur " +
            "    JOIN roles r ON ur.role_id = r.id " +
            "    WHERE ur.user_id = u.id AND r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR') " +
            ") " +
            "ORDER BY `rank` ASC, u.username ASC",
            countQuery = "SELECT count(*) FROM users u " +
                    "WHERE u.status = 'ACTIVE' " +
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM user_roles ur " +
                    "    JOIN roles r ON ur.role_id = r.id " +
                    "    WHERE ur.user_id = u.id AND r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR') " +
                    ")",
            nativeQuery = true)
    Page<UserRankingProjection> getGlobalRankingOI(Pageable pageable);


    @Query(value = "SELECT " +
            "u.id AS userId, u.username AS username, u.avatar_url AS avatarUrl, " +
            "u.`total-score` AS totalScore, u.solved_count AS solvedCount, u.ac_count AS acCount, u.submission_count AS submissionCount, " +
            "RANK() OVER (ORDER BY u.solved_count DESC, u.`total-score` DESC, u.ac_count DESC, u.submission_count ASC) AS `rank` " +
            "FROM users u " +
            "WHERE u.status = 'ACTIVE' " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM user_roles ur " +
            "    JOIN roles r ON ur.role_id = r.id " +
            "    WHERE ur.user_id = u.id AND r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR') " +
            ") " +
            "ORDER BY `rank` ASC, u.username ASC",
            countQuery = "SELECT count(*) FROM users u " +
                    "WHERE u.status = 'ACTIVE' " +
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM user_roles ur " +
                    "    JOIN roles r ON ur.role_id = r.id " +
                    "    WHERE ur.user_id = u.id AND r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR') " +
                    ")",
            nativeQuery = true)
    Page<UserRankingProjection> getGlobalRankingACM(Pageable pageable);

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
