package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.contests.ContestLeaderboardSdo;
import com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo;
import com.kma.ojcore.dto.response.contests.ContestParticipationSdo;
import com.kma.ojcore.dto.response.contests.MyActiveContestSdo;
import com.kma.ojcore.entity.ContestParticipation;
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
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, UUID> {

    boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

    long countByContestId(UUID contestId);

    Optional<ContestParticipation> findByContestIdAndUserId(UUID contestId, UUID userId);

    @Query("SELECT new com.kma.ojcore.dto.response.contests.MyActiveContestSdo(" +
            "new com.kma.ojcore.dto.response.contests.ContestBasicSdo(" +
            "c.id, c.title, c.startTime, c.endTime, c.ruleType, null, c.visibility, " +
            "(SELECT COUNT(cp2) FROM ContestParticipation cp2 WHERE cp2.contest.id = c.id), " +
            "c.status, c.durationMinutes), " +
            "p.endTime) " +
            "FROM ContestParticipation p " +
            "JOIN p.contest c " +
            "WHERE p.user.id = :userId " +
            "AND p.isFinished = false " +
            "AND p.startTime IS NOT NULL " +
            "AND p.endTime > CURRENT_TIMESTAMP " +
            "AND c.status = 'ACTIVE'")
    List<MyActiveContestSdo> findMyActiveContestSdos(@Param("userId") UUID userId);


    @Modifying
    @Query("UPDATE ContestParticipation cp SET cp.isDisqualified = true WHERE cp.contest.id = :contestId AND cp.user.id IN :userIds")
    int banUsersInBulk(@Param("contestId") UUID contestId,
                       @Param("userIds") List<UUID> userIds);

    @Modifying
    @Query("UPDATE ContestParticipation cp SET cp.isDisqualified = false WHERE cp.contest.id = :contestId AND cp.user.id IN :userIds")
    int unbanUsersInBulk(@Param("contestId") UUID contestId, @Param("userIds") List<UUID> userIds);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipationSdo(" +
            "cp.user.id, cp.user.username, cp.user.email, cp.isDisqualified, cp.startTime, cp.endTime, cp.isFinished, cp.score, cp.penalty) " +
            "FROM ContestParticipation cp " +
            "WHERE cp.contest.id = :contestId " +
            "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
            "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)",
            countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp " +
                    "WHERE cp.contest.id = :contestId " +
                    "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
                    "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
                    "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)")
    Page<ContestParticipationSdo> searchParticipants(@Param("contestId") UUID contestId,
                                                     @Param("keyword") String keyword,
                                                     @Param("isDisqualified") Boolean isDisqualified,
                                                     Pageable pageable);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo(" +
            "cp.user.id, cp.user.username) " +
            "FROM ContestParticipation cp " +
            "WHERE cp.contest.id = :contestId " +
            "AND cp.isDisqualified = false " +
            "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')",
            countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp " +
                    "WHERE cp.contest.id = :contestId " +
                    "AND cp.isDisqualified = false " +
                    "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
    Page<ContestParticipantPublicSdo> searchPublicParticipants(@Param("contestId") UUID contestId,
                                                               @Param("keyword") String keyword,
                                                               Pageable pageable);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestLeaderboardSdo(" +
            "cp.user.id, cp.user.username, cp.score, cp.penalty) " +
            "FROM ContestParticipation cp " +
            "WHERE cp.contest.id = :contestId AND cp.isDisqualified = false " +
            "ORDER BY cp.score DESC, cp.penalty ASC",
            countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp " +
                    "WHERE cp.contest.id = :contestId AND cp.isDisqualified = false")
    Page<ContestLeaderboardSdo> getLeaderboard(@Param("contestId") UUID contestId, Pageable pageable);
}