package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, UUID> {

    List<ContestProblem> findByContestIdOrderBySortOrderAsc(UUID contestId);

    boolean existsByContestIdAndProblemId(UUID contestId, UUID problemId);

    boolean existsByContestIdAndDisplayId(UUID contestId, String displayId);
}