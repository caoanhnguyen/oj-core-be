package com.kma.ojcore.service.scoring;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcmScoringStrategy implements ContestScoringStrategy {

    private final SubmissionRepository submissionRepository;

    @Override
    public void processScore(Submission submission, ContestParticipation participation) {
        // ACM chỉ quan tâm khi làm đúng (AC)
        if (submission.getVerdict() != SubmissionVerdict.AC) return;

        // 1. Kiểm tra xem bài này trước đó User đã AC trong kỳ thi chưa? (Chống cheat nộp 1 bài 2 lần lấy điểm)
        boolean alreadySolved = submissionRepository.existsByContestIdAndUserIdAndProblemIdAndVerdictAndCreatedDateBefore(
                submission.getContest().getId(),
                submission.getUser().getId(),
                submission.getProblem().getId(),
                SubmissionVerdict.AC,
                submission.getCreatedDate()
        );
        if (alreadySolved) return;

        // 2. Tính thời gian từ lúc bắt đầu thi đến lúc AC (tính bằng Phút)
        long minutesToAc = Duration.between(submission.getContest().getStartTime(), submission.getCreatedDate()).toMinutes();

        // 3. Đếm số lần nộp sai (WA, TLE, MLE, CE...) TRƯỚC cái submission AC này
        // TODO: check cái submission này có vấn đề N+1 không
        long failedAttempts = submissionRepository.countFailedAttemptsBeforeAc(
                submission.getContest().getId(),
                submission.getUser().getId(),
                submission.getProblem().getId(),
                submission.getCreatedDate()
        );

        // 4. Công thức Penalty chuẩn ACM: 1 lần sai phạt 20 phút
        // TODO: config số phút phạn penalty vào application.yml
        long penaltyForThisProblem = minutesToAc + (failedAttempts * 20);

        // 5. Cập nhật vào Bảng thành tích
        participation.setScore(participation.getScore() + 1.0);
        participation.setPenalty(participation.getPenalty() + penaltyForThisProblem);

        log.info("ACM Score updated for User {}: +1 point, Penalty +{}", submission.getUser().getUsername(), penaltyForThisProblem);
    }
}