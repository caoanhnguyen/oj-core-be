package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    UUID user(User user);

    interface VerdictCountProjection {
        String getVerdict();
        Long getCount();
    }

    interface ProblemVerdictProjection {
        UUID getProblemId();
        SubmissionVerdict getVerdict();
    }

    @Query("SELECT s.problem.id as problemId, s.verdict as verdict " +
            "FROM Submission s " +
            "WHERE s.contest.id = :contestId AND s.user.id = :userId")
    List<ProblemVerdictProjection> findVerdictsByContestAndUser(@Param("contestId") UUID contestId,
                                                                @Param("userId") UUID userId);

    @Query("SELECT s.createdDate FROM Submission s " +
            "WHERE s.user.id = :userId " +
            "AND s.createdDate >= :startDate " +
            "ORDER BY s.createdDate DESC")
    List<LocalDateTime> findSubmissionDatesByUserIdAndStartDate(UUID userId, LocalDateTime startDate);

    @Query("SELECT s.verdict AS verdict, COUNT(s.id) AS count " +
            "FROM Submission s " +
            "WHERE s.problem.id = :problemId " +
            "AND NOT EXISTS (SELECT 1 FROM s.user.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR')) " +
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
            "FROM Submission s LEFT JOIN s.contest c " +
            "WHERE (:problemId IS NULL OR s.problem.id = :problemId) " +
            "AND (:userId IS NULL OR s.user.id = :userId) " +
            "AND (:submissionVerdict IS NULL OR s.verdict = :submissionVerdict) " +
            "AND (:keyword IS NULL OR LOWER(s.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
            "                      OR LOWER(s.problem.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
            "AND (:status IS NULL OR s.problem.status = :status) " +
            "AND (:problemStatus IS NULL OR s.problem.problemStatus = :problemStatus) " +
            "AND (s.verdict IN :verdicts) " +
            "AND (c IS NULL OR c.endTime < CURRENT_TIMESTAMP) " +
            "AND (:hideStaff = false OR NOT EXISTS (SELECT 1 FROM s.user.roles r WHERE r.name IN ('ROLE_ADMIN', 'ROLE_MODERATOR')))")
    Page<SubmissionBasicSdo> getSubmissions(@Param("problemId") UUID problemId,
                                            @Param("userId") UUID userId,
                                            @Param("submissionVerdict") SubmissionVerdict submissionVerdict,
                                            @Param("keyword") String keyword,
                                            @Param("status") EStatus status,
                                            @Param("problemStatus") ProblemStatus problemStatus,
                                            @Param("verdicts") List<SubmissionVerdict> allowedVerdicts,
                                            @Param("hideStaff") boolean hideStaff,
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

    @Query("SELECT s FROM Submission s WHERE s.verdict = 'PENDING' AND s.updatedDate < :threshold")
    List<Submission> findStuckSubmissions(@Param("threshold") LocalDateTime threshold);

    // 1. Dùng cho ACM: Kiểm tra xem trước đó đã AC bài này chưa?
    boolean existsByContestIdAndUserIdAndProblemIdAndVerdictAndCreatedDateBefore(
            UUID contestId, UUID userId, UUID problemId, SubmissionVerdict verdict, java.time.LocalDateTime createdDate);

    // 2. Dùng cho ACM: Đếm số lần nộp sai (Khác AC) trước khi AC
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.contest.id = :contestId " +
            "AND s.user.id = :userId AND s.problem.id = :problemId " +
            "AND s.verdict <> com.kma.ojcore.enums.SubmissionVerdict.AC " +
            "AND s.createdDate < :createdDate")
    long countFailedAttemptsBeforeAc(@Param("contestId") UUID contestId,
                                     @Param("userId") UUID userId,
                                     @Param("problemId") UUID problemId,
                                     @Param("createdDate") java.time.LocalDateTime createdDate);

    // 3. Dùng cho OI: Tính tổng điểm bằng cách lấy MAX điểm của từng bài rồi cộng lại
    @Query("SELECT MAX(s.score) FROM Submission s " +
            "WHERE s.contest.id = :contestId AND s.user.id = :userId " +
            "AND s.createdDate <= s.contest.endTime " +
            "GROUP BY s.problem.id")
    List<Double> findMaxScoresPerProblem(@Param("contestId") UUID contestId, @Param("userId") UUID userId);

    // Dùng cho OI theo chuẩn xịn: Scale điểm từ Judger sang hệ số Contest (RawScore / BaseScore) * ContestProblemPoints
    @Query("SELECT MAX((CAST(COALESCE(s.score, 0) AS double) / COALESCE(p.totalScore, 100.0)) * cp.points) " +
            "FROM Submission s " +
            "JOIN s.problem p " +
            "JOIN ContestProblem cp ON s.contest.id = cp.contest.id AND s.problem.id = cp.problem.id " +
            "WHERE s.contest.id = :contestId AND s.user.id = :userId " +
            "AND s.createdDate <= s.contest.endTime " +
            "GROUP BY s.problem.id")
    List<Double> findMaxScaledScoresPerProblem(@Param("contestId") UUID contestId, @Param("userId") UUID userId);

    // 1. Dùng cho User: Lấy danh sách bài nộp CỦA CHÍNH HỌ trong một Contest cụ thể
    @Query("SELECT new com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo(" +
            "s.id, s.verdict, s.score, s.passedTestCount, s.totalTestCount, " +
            "s.executionTimeMs, s.executionMemoryMb, s.createdDate, s.languageKey, " +
            "s.user.id, s.user.username, s.problem.id, s.problem.title, s.problem.slug) " +
            "FROM Submission s JOIN s.problem p " +
            "WHERE s.contest.id = :contestId AND s.user.id = :userId " +
            "AND (:problemId IS NULL OR s.problem.id = :problemId) " +
            "ORDER BY s.createdDate DESC")
    Page<SubmissionBasicSdo> findMyContestSubmissions(@Param("contestId") UUID contestId,
                                                      @Param("userId") UUID userId,
                                                      @Param("problemId") UUID problemId,
                                                      Pageable pageable);

    // 2. Dùng cho Admin: Lấy danh sách TẤT CẢ bài nộp của TẤT CẢ mọi người trong Contest
    @Query("SELECT new com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo(" +
            "s.id, s.verdict, s.score, s.passedTestCount, s.totalTestCount, " +
            "s.executionTimeMs, s.executionMemoryMb, s.createdDate, s.languageKey, " +
            "s.user.id, s.user.username, s.problem.id, s.problem.title, s.problem.slug) " +
            "FROM Submission s JOIN s.problem p " +
            "WHERE s.contest.id = :contestId " +
            "ORDER BY s.createdDate DESC")
    Page<SubmissionBasicSdo> findAllContestSubmissions(@Param("contestId") UUID contestId,
                                                       Pageable pageable);
}