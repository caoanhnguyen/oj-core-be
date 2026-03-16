package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    interface VerdictCountProjection {
        String getVerdict(); // Trả về tên Enum dạng String
        Long getCount();
    }

    @Query("SELECT s.verdict AS verdict, COUNT(s.id) AS count " +
            "FROM Submission s " +
            "WHERE s.problem.id = :problemId " +
            "GROUP BY s.verdict")
    List<VerdictCountProjection> countSubmissionsByVerdict(@Param("problemId") UUID problemId);

    @Query("SELECT new com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo(" +
            "s.id, s.user.id, s.user.username, s.problem.id, s.problem.title, s.problem.slug, " +
            "s.languageKey, s.submissionStatus, s.verdict, s.score, " +
            "s.passedTestCount, s.totalTestCount, " +
            "s.executionTimeMs, s.executionMemoryMb, s.createdDate, " +
            "s.errorMessage, s.sourceCode) " +
            "FROM Submission s " +
            "WHERE s.id = :submissionId")
    SubmissionDetailsSdo getDetails(UUID submissionId);

    @Query("SELECT new com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo(" +
            "s.id, s.verdict, s.score, s.passedTestCount, s.totalTestCount, " +
            "s.executionTimeMs, s.executionMemoryMb, s.createdDate, s.languageKey, " +
            "s.user.id, s.user.username, s.problem.id, s.problem.title, s.problem.slug) " +
            "FROM Submission s " +
            "WHERE (:problemId IS NULL OR s.problem.id = :problemId) " +
            "AND (:userId IS NULL OR s.user.id = :userId) " +
            "AND (:submissionVerdict IS NULL OR s.verdict = :submissionVerdict) " +
            "AND (:keyword IS NULL OR LOWER(s.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                      OR LOWER(s.problem.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:problemStatus IS NULL OR s.problem.status = :status)" +
            "AND (:problemStatus IS NULL OR s.problem.problemStatus = :problemStatus)" +
            "AND (s.verdict IN :verdicts)")
    Page<SubmissionBasicSdo> getSubmissions(@Param("problemId") UUID problemId,
                                            @Param("userId") UUID userId,
                                            @Param("submissionVerdict") SubmissionVerdict submissionVerdict,
                                            @Param("keyword") String keyword,
                                            @Param("status") EStatus status,
                                            @Param("problemStatus") ProblemStatus problemStatus,
                                            @Param("verdicts") List<SubmissionVerdict> allowedVerdicts,
                                            Pageable pageable);



    @Query(value = "SELECT source_code FROM submissions " +
            "WHERE problem_id = :problemId " +
            "AND user_id = :userId " +
            "AND language_key = :languageKey " +
            "ORDER BY created_date DESC LIMIT 1",
            nativeQuery = true)
    Optional<String> findFirstSourceCodeByProblemIdAndUserIdAndLanguageKey(
            @Param("problemId") UUID problemId,
            @Param("userId") UUID userId,
            @Param("languageKey") String languageKey
    );
}