package com.kma.ojcore.service.scoring;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.ContestParticipationProblem;
import com.kma.ojcore.entity.ContestProblem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.repository.ContestParticipationProblemRepository;
import com.kma.ojcore.repository.ContestParticipationRepository;
import com.kma.ojcore.repository.ContestProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OiScoringStrategy implements ContestScoringStrategy {

    private final ContestParticipationProblemRepository cppRepository;
    private final ContestParticipationRepository participationRepository;
    private final ContestProblemRepository contestProblemRepository;

    @Override
    @Transactional
    public void processScore(Submission submission, ContestParticipation participation) {
        if (submission.getScore() == null) return;

        ContestProblem cp = contestProblemRepository.findByContestIdAndProblemId(
            submission.getContest().getId(), 
            submission.getProblem().getId()
        ).orElse(null);

        if (cp == null) return;

        double maxContestPoints = cp.getPoints() != null ? cp.getPoints() : 100.0;
        double totalProblemScore = submission.getProblem().getTotalScore() != null ? submission.getProblem().getTotalScore() : 100.0;

        double newScaledScore = (submission.getScore() / totalProblemScore) * maxContestPoints;
        long newPenaltyMins = Math.max(0, Duration.between(participation.getStartTime(), submission.getCreatedDate()).toMinutes());

        ContestParticipationProblem cpp = cppRepository.findByParticipationAndContestProblem(participation, cp)
            .orElseGet(() -> ContestParticipationProblem.builder()
                .participation(participation)
                .contestProblem(cp)
                .maxScore(0.0)
                .penalty(Long.MAX_VALUE)
                .failedAttempts(0)
                .isAc(false)
                .build());

        double oldScore = cpp.getMaxScore();
        long oldPenalty = cpp.getPenalty();

        boolean isScoreImproved = newScaledScore > oldScore;
        boolean isPenaltyImproved = (newScaledScore == oldScore) && (newPenaltyMins < oldPenalty) && (newScaledScore > 0);

        if (isScoreImproved || isPenaltyImproved) {
            double scoreDiff = newScaledScore - oldScore;
            
            long oldEffectivePenalty = (oldScore > 0 && oldPenalty != Long.MAX_VALUE) ? oldPenalty : 0;
            long newEffectivePenalty = (newScaledScore > 0) ? newPenaltyMins : 0;
            long penaltyDiff = newEffectivePenalty - oldEffectivePenalty;

            cpp.setMaxScore(newScaledScore);
            cpp.setPenalty(newEffectivePenalty > 0 ? newEffectivePenalty : Long.MAX_VALUE);
            if (newScaledScore >= maxContestPoints) {
                cpp.setIsAc(true);
            }
            cppRepository.save(cpp);

            participationRepository.addScoreAndPenalty(participation.getId(), scoreDiff, penaltyDiff);

            log.info("OI Score updated for User {}: +{} points, Penalty diff: {}", submission.getUser().getUsername(), scoreDiff, penaltyDiff);
        }
    }
}