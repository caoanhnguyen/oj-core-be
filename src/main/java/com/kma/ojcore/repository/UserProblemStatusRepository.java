package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.topics.DifficultyCountProjection;
import com.kma.ojcore.entity.UserProblemStatus;
import com.kma.ojcore.enums.UserProblemState;
import org.springframework.data.jpa.repository.JpaRepository;
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
}