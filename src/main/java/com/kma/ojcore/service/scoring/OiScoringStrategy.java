package com.kma.ojcore.service.scoring;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OiScoringStrategy implements ContestScoringStrategy {

    private final SubmissionRepository submissionRepository;

    @Override
    public void processScore(Submission submission, ContestParticipation participation) {
        if (submission.getScore() == null) return;

        // 1. Lấy danh sách các điểm số cao nhất của từng bài
        // Chú ý: Ở đây ta dùng hàm cũ vì bản nộp Submission ĐÃ ĐƯỢC scale điểm trực tiếp trong JudgeResultListener
        List<Double> maxScores = submissionRepository.findMaxScoresPerProblem(
                submission.getContest().getId(),
                submission.getUser().getId()
        );

        // 2. Dùng Java Stream để cộng tổng lại (loại bỏ các giá trị null nếu có)
        double totalOiScore = maxScores.stream()
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        // 3. Cập nhật vào DB
        participation.setScore(totalOiScore);
        participation.setPenalty(0L);

        log.info("OI Score updated for User {}: Total Score = {}", submission.getUser().getUsername(), totalOiScore);
    }
}