package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    boolean existsBySlug(String slug);

    Optional<Problem> findBySlug(String slug);

    Optional<Problem> findById(UUID id);

    @Query("SELECT new com.kma.ojcore.dto.response.problems.ProblemResponse(" +
            "p.id, p.title, p.slug, p.difficulty, p.status, p.problemStatus, " +
            "p.submissionCount, p.acceptedCount, p.totalScore, p.ruleType, null, null, p.createdDate, p.updatedDate) " +
            "FROM Problem p " +
            "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:difficulty IS NULL OR p.difficulty = :difficulty) " +
            "AND (:ruleType IS NULL OR p.ruleType = :ruleType) " +
            "AND (:topicSlugs IS NULL OR EXISTS (SELECT t FROM p.topics t WHERE t.slug IN :topicSlugs)) " +
            "AND (:status IS NULL OR p.status = :status)" +
            "AND (:problemStatus IS NULL OR p.problemStatus = :problemStatus)")
    Page<ProblemResponse> searchProblems(@Param("keyword") String keyword,
                                         @Param("difficulty") ProblemDifficulty difficulty,
                                         @Param("ruleType") RuleType ruleType,
                                         @Param("topicSlugs") List<String> topicSlugs,
                                         @Param("status") EStatus status,
                                         @Param("problemStatus") ProblemStatus problemStatus,
                                         Pageable pageable);




    @Modifying
    @Query("Update Problem p set p.status = :status where p.id = :id")
    void updateStatusById(@Param("status") EStatus status, @Param("id") UUID id);

    @Modifying
    @Query("Update Problem p set p.problemStatus = :problemStatus where p.id = :id")
    void updateProblemStatusById(@Param("problemStatus") ProblemStatus problemStatus, @Param("id") UUID id);

    long countByIdInAndStatusNot(Collection<UUID> ids, EStatus status);

    @Modifying
    @Query("UPDATE Problem p SET p.acceptedCount = p.acceptedCount + 1 WHERE p.id = :problemId")
    void incrementAcceptedCount(@Param("problemId") UUID problemId);

    @Modifying
    @Query("UPDATE Problem p SET p.submissionCount = p.submissionCount + 1 WHERE p.id = :problemId")
    void incrementSubmissionCount(@Param("problemId") UUID problemId);

    @Modifying
    @Query(value = "UPDATE problems p " +
            "SET accepted_count = (SELECT COUNT(*) FROM submissions WHERE problem_id = p.id AND verdict = 'AC' AND status = 'ACTIVE'), " +
            "submission_count = (SELECT COUNT(*) FROM submissions WHERE problem_id = p.id AND status = 'ACTIVE') " +
            "WHERE p.id = :problemId", nativeQuery = true)
    int recalculateProblemStats(@Param("problemId") UUID problemId);
}
