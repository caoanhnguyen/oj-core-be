package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.topics.DifficultyCountProjection;
import com.kma.ojcore.entity.UserProblemStatus;
import com.kma.ojcore.enums.UserProblemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProblemStatusRepository extends JpaRepository<UserProblemStatus, UUID> {

    Optional<UserProblemStatus> findByUserIdAndProblemId(UUID userId, UUID problemId);

    List<UserProblemStatus> findByUserIdAndProblemIdIn(UUID userId, List<UUID> problemIds);

    @Query("SELECT COUNT(ups) FROM UserProblemStatus ups " +
            "WHERE ups.user.id = :userId " +
            "AND ups.state = :state " +
            "AND ups.problem.problemStatus = 'PUBLISHED'")
    long countByUserIdAndState(UUID userId, UserProblemState state);

    @Query("SELECT p.difficulty AS difficulty, COUNT(ups.id) AS count " +
            "FROM UserProblemStatus ups " +
            "JOIN ups.problem p " +
            "JOIN p.topics t " +
            "WHERE ups.user.id = :userId " +
            "AND t.slug = :slug " +
            "AND ups.state = 'SOLVED' " +
            "AND p.status = 'ACTIVE' " +
            "AND p.problemStatus = 'PUBLISHED' " +
            "GROUP BY p.difficulty")
    List<DifficultyCountProjection> countSolvedProblemsByDifficultyForTopic(
            @Param("userId") UUID userId,
            @Param("slug") String slug);

    @Modifying
    @Query(value = "UPDATE user_problem_status ups " +
            "SET max_score = COALESCE(( " +
            "    SELECT MAX(s.score) " +
            "    FROM submissions s " +
            "    WHERE s.user_id = ups.user_id AND s.problem_id = ups.problem_id AND s.contest_id IS NULL AND s.verdict != 'CE' AND s.verdict != 'SE' AND s.status = 'ACTIVE' " +
            "), 0), " +
            "state = CASE WHEN EXISTS ( " +
            "    SELECT 1 FROM submissions s2 " +
            "    WHERE s2.user_id = ups.user_id AND s2.problem_id = ups.problem_id AND s2.contest_id IS NULL AND s2.verdict = 'AC' AND s2.status = 'ACTIVE' " +
            ") THEN 'SOLVED' ELSE 'ATTEMPTED' END " +
            "WHERE ups.user_id = :userId AND ups.problem_id = :problemId", nativeQuery = true)
    int recalculateStatus(@Param("userId") UUID userId, @Param("problemId") UUID problemId);
}