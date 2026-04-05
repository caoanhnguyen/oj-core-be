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
            "cp.id, p.id, p.slug, p.title, cp.displayId, cp.points, cp.sortOrder, p.status, null) " +
            "FROM ContestProblem cp JOIN cp.problem p " +
            "WHERE cp.contest.id = :contestId " +
            "ORDER BY cp.sortOrder ASC")
    List<ContestProblemSdo> findByContestIdOrderBySortOrderAsc(UUID contestId);

    @Query("SELECT cp FROM ContestProblem cp JOIN FETCH cp.problem p WHERE cp.contest.id = :contestId")
    List<ContestProblem> findByContestId(UUID contestId);

    @Modifying
    @Query("DELETE FROM ContestProblem cp WHERE cp.contest.id = :contestId AND cp.problem.id IN :problemIds")
    void deleteByContestIdAndProblemIdIn(@Param("contestId") UUID contestId, @Param("problemIds") List<UUID> problemIds);

    boolean existsByContestIdAndProblemId(UUID contestId, UUID problemId);

    @Query("SELECT cp.problem.id FROM ContestProblem cp WHERE cp.contest.id = :contestId")
    List<UUID> findProblemIdsByContestId(@Param("contestId") UUID contestId);

    @Query("SELECT cp.points FROM ContestProblem cp WHERE cp.contest.id = :contestId AND cp.problem.id = :problemId")
    Integer findPointsByContestIdAndProblemId(@Param("contestId") UUID contestId, @Param("problemId") UUID problemId);
}