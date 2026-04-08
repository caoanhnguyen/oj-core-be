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
    private final com.kma.ojcore.repository.ContestProblemRepository contestProblemRepository;

    @Override
    public void processScore(Submission submission, ContestParticipation participation) {
        if (submission.getScore() == null) return;

        List<Submission> validSubmissions = submissionRepository.findValidSubmissionsByContestIdAndUserId(
                submission.getContest().getId(),
                submission.getUser().getId()
        );

        List<com.kma.ojcore.entity.ContestProblem> contestProblems = contestProblemRepository.findByContestId(submission.getContest().getId());
        java.util.Map<java.util.UUID, Double> problemPointsMap = new java.util.HashMap<>();
        for (com.kma.ojcore.entity.ContestProblem cp : contestProblems) {
            problemPointsMap.put(cp.getProblem().getId(), cp.getPoints() != null ? Double.valueOf(cp.getPoints()) : 100.0);
        }

        double totalOiScore = 0.0;
        long totalPenaltyMinutes = 0L;

        // Group submissions by problem
        java.util.Map<java.util.UUID, List<Submission>> subsByProblem = validSubmissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getProblem().getId()));

        for (java.util.Map.Entry<java.util.UUID, List<Submission>> entry : subsByProblem.entrySet()) {
            java.util.UUID problemId = entry.getKey();
            List<Submission> subs = entry.getValue();

            double maxScaledScore = 0.0;
            long earliestPenaltyForMaxScore = Long.MAX_VALUE;

            for (Submission s : subs) {
                double rawScore = s.getScore() != null ? s.getScore() : 0.0;
                double totalScore = s.getProblem().getTotalScore() != null ? s.getProblem().getTotalScore() : 100.0;
                double contestWeight = problemPointsMap.getOrDefault(problemId, 100.0);
                double scaledScore = (rawScore / totalScore) * contestWeight;

                long penaltyMins = java.time.Duration.between(participation.getStartTime(), s.getCreatedDate()).toMinutes();
                if (penaltyMins < 0) penaltyMins = 0;

                if (scaledScore > maxScaledScore) {
                    maxScaledScore = scaledScore;
                    earliestPenaltyForMaxScore = penaltyMins;
                } else if (scaledScore == maxScaledScore && scaledScore > 0) {
                    if (penaltyMins < earliestPenaltyForMaxScore) {
                        earliestPenaltyForMaxScore = penaltyMins;
                    }
                }
            }

            totalOiScore += maxScaledScore;
            if (maxScaledScore > 0 && earliestPenaltyForMaxScore != Long.MAX_VALUE) {
                totalPenaltyMinutes += earliestPenaltyForMaxScore;
            }
        }

        participation.setScore(totalOiScore);
        participation.setPenalty(totalPenaltyMinutes);

        log.info("OI Score updated for User {}: Total Score = {}, Penalty = {}", submission.getUser().getUsername(), totalOiScore, totalPenaltyMinutes);
    }
}