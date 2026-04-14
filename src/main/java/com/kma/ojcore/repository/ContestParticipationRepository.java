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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, UUID> {

        boolean existsByContestIdAndUserId(UUID contestId, UUID userId);

        boolean existsByContestContestKeyAndUserId(String contestKey, UUID userId);

        long countByContestContestKey(String contestKey);

        List<ContestParticipation> findAllByContestId(UUID contestId);

        Optional<ContestParticipation> findByContestContestKeyAndUserId(String contestKey, UUID userId);

        Optional<ContestParticipation> findByContestIdAndUserId(UUID contestId, UUID userId);

        @Modifying
        @Query("UPDATE ContestParticipation cp SET cp.score = cp.score + :deltaScore, cp.penalty = cp.penalty + :deltaPenalty WHERE cp.id = :participationId")
        void addScoreAndPenalty(@Param("participationId") UUID participationId, @Param("deltaScore") Double deltaScore, @Param("deltaPenalty") Long deltaPenalty);

        @Query("SELECT new com.kma.ojcore.dto.response.contests.MyActiveContestSdo(" +
                        "new com.kma.ojcore.dto.response.contests.ContestBasicSdo(" +
                        "c.id, c.title, c.contestKey, c.startTime, c.endTime, c.ruleType, null, c.visibility, " +
                        "(SELECT COUNT(cp2) FROM ContestParticipation cp2 WHERE cp2.contest.id = c.id), " +
                        "c.status, c.durationMinutes, c.format, c.allowLateRegistration, c.scoreboardVisibility, c.resourceVisibility), " +
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

        @Modifying
        @Transactional
        @Query(value = "UPDATE contest_participations cp " +
                        "SET score = ( " +
                        "    SELECT COALESCE(SUM(max_score), 0) " +
                        "    FROM ( " +
                        "        SELECT s.user_id, MAX((CAST(COALESCE(s.score, 0) AS float) / COALESCE(p.total_score, 100.0)) * c_p.points) as max_score "
                        +
                        "        FROM submissions s " +
                        "        JOIN problems p ON p.id = s.problem_id " +
                        "        JOIN contest_problems c_p ON c_p.problem_id = s.problem_id AND c_p.contest_id = s.contest_id "
                        +
                        "        WHERE s.contest_id = :contestId AND s.status = 'ACTIVE' " +
                        "        GROUP BY s.user_id, s.problem_id " +
                        "    ) user_problem_scores " +
                        "    WHERE user_problem_scores.user_id = cp.user_id " +
                        ") " +
                        "WHERE cp.contest_id = :contestId", nativeQuery = true)
        int recalculateOiScoresByContestId(@Param("contestId") UUID contestId);

        @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipationSdo(" +
                        "cp.user.id, cp.user.username, cp.user.email, cp.isDisqualified, cp.startTime, cp.endTime, cp.isFinished, cp.score, cp.penalty) "
                        +
                        "FROM ContestParticipation cp " +
                        "WHERE cp.contest.contestKey = :contestKey " +
                        "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' "
                        +
                        "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
                        "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)", countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp "
                                        +
                                        "WHERE cp.contest.contestKey = :contestKey " +
                                        "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' "
                                        +
                                        "     OR LOWER(cp.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') "
                                        +
                                        "AND (:isDisqualified IS NULL OR cp.isDisqualified = :isDisqualified)")
        Page<ContestParticipationSdo> searchParticipants(@Param("contestKey") String contestKey,
                        @Param("keyword") String keyword,
                        @Param("isDisqualified") Boolean isDisqualified,
                        Pageable pageable);

        @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo(" +
                        "cp.user.id, cp.user.username) " +
                        "FROM ContestParticipation cp " +
                        "WHERE cp.contest.contestKey = :contestKey " +
                        "AND cp.isDisqualified = false " +
                        "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')", countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp "
                                        +
                                        "WHERE cp.contest.contestKey = :contestKey " +
                                        "AND cp.isDisqualified = false " +
                                        "AND (:keyword IS NULL OR LOWER(cp.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')")
        Page<ContestParticipantPublicSdo> searchPublicParticipants(@Param("contestKey") String contestKey,
                                                                   @Param("keyword") String keyword,
                                                                   Pageable pageable);

        interface ContestLeaderboardProjection {
                byte[] getUserId();
                String getUsername();
                Double getScore();
                Long getPenalty();
                Integer getRank();
        }

        @Query(value = "SELECT cp.user_id AS userId, u.username AS username, cp.score AS score, cp.penalty AS penalty, " +
                        "CAST(RANK() OVER (ORDER BY cp.score DESC, cp.penalty ASC) AS UNSIGNED) AS `rank` " +
                        "FROM contest_participations cp " +
                        "JOIN users u ON cp.user_id = u.id " +
                        "WHERE cp.contest_id = :contestId AND cp.is_disqualified = false " +
                        "ORDER BY cp.score DESC, cp.penalty ASC", countQuery = "SELECT COUNT(*) FROM contest_participations "
                                        +
                                        "WHERE contest_id = :contestId AND is_disqualified = false", nativeQuery = true)
        Page<ContestLeaderboardProjection> getLeaderboardNative(@Param("contestId") UUID contestId, Pageable pageable);

        @Query(value = "SELECT new com.kma.ojcore.dto.response.contests.ContestLeaderboardSdo(" +
                        "cp.user.id, cp.user.username, cp.score, cp.penalty) " +
                        "FROM ContestParticipation cp " +
                        "WHERE cp.contest.id = :contestId AND cp.isDisqualified = false " +
                        "ORDER BY cp.score DESC, cp.penalty ASC", countQuery = "SELECT COUNT(cp) FROM ContestParticipation cp "
                                        +
                                        "WHERE cp.contest.id = :contestId AND cp.isDisqualified = false")
        Page<ContestLeaderboardSdo> getLeaderboard(@Param("contestId") UUID contestId, Pageable pageable);
}