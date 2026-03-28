package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.contests.ContestProblemSdo;
import com.kma.ojcore.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, UUID> {

    @Query("SELECT new com.kma.ojcore.dto.response.contests.ContestProblemSdo(" +
            "cp.id, p.id, p.slug, p.title, cp.displayId, cp.points, cp.sortOrder) " +
            "FROM ContestProblem cp JOIN cp.problem p " +
            "WHERE cp.contest.id = :contestId " +
            "ORDER BY cp.sortOrder ASC")
    List<ContestProblemSdo> findByContestIdOrderBySortOrderAsc(UUID contestId);

    @Modifying
    @Query("DELETE FROM ContestProblem cp WHERE cp.contest.id = :contestId AND cp.problem.id = :problemId")
    int deleteByContestIdAndProblemId(@Param("contestId") UUID contestId,
                                      @Param("problemId") UUID problemId);

    interface ConflictProjection {
        UUID getProblemId();
        String getDisplayId();
    }

    @Query("SELECT cp.problem.id as problemId, cp.displayId as displayId " +
            "FROM ContestProblem cp " +
            "WHERE cp.contest.id = :contestId AND (cp.problem.id = :problemId OR cp.displayId = :displayId)")
    List<ConflictProjection> findConflicts(@Param("contestId") UUID contestId,
                                           @Param("problemId") UUID problemId,
                                           @Param("displayId") String displayId);

    @Modifying
    @Query("DELETE FROM ContestProblem cp WHERE cp.contest.id = :contestId AND cp.problem.id IN :problemIds")
    int deleteByContestIdAndProblemIdIn(@Param("contestId") UUID contestId, @Param("problemIds") List<UUID> problemIds);

}