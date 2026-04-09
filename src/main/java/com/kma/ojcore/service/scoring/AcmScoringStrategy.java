package com.kma.ojcore.service.scoring;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.ContestParticipationProblem;
import com.kma.ojcore.entity.ContestProblem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.SubmissionVerdict;
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
public class AcmScoringStrategy implements ContestScoringStrategy {

    private final ContestParticipationProblemRepository cppRepository;
    private final ContestParticipationRepository participationRepository;
    private final ContestProblemRepository contestProblemRepository;

    @Override
    @Transactional
    public void processScore(Submission submission, ContestParticipation participation) {
        ContestProblem cp = contestProblemRepository.findByContestIdAndProblemId(
            submission.getContest().getId(), 
            submission.getProblem().getId()
        ).orElse(null);

        if (cp == null) return;

        ContestParticipationProblem cpp = cppRepository.findByParticipationAndContestProblem(participation, cp)
            .orElseGet(() -> ContestParticipationProblem.builder()
                .participation(participation)
                .contestProblem(cp)
                .maxScore(0.0)
                .penalty(0L)
                .failedAttempts(0)
                .isAc(false)
                .build());

        if (cpp.getIsAc()) return;

        if (submission.getVerdict() != SubmissionVerdict.AC && submission.getVerdict() != SubmissionVerdict.CE) {
            cpp.setFailedAttempts(cpp.getFailedAttempts() + 1);
            cppRepository.save(cpp);
            return;
        }

        if (submission.getVerdict() == SubmissionVerdict.AC) {
            long minutesToAc = Duration.between(participation.getStartTime(), submission.getCreatedDate()).toMinutes();
            long penaltyForThisProblem = minutesToAc + (cpp.getFailedAttempts() * 20L);

            cpp.setIsAc(true);
            cpp.setMaxScore(1.0);
            cpp.setPenalty(penaltyForThisProblem);
            cppRepository.save(cpp);

            participationRepository.addScoreAndPenalty(participation.getId(), 1.0, penaltyForThisProblem);

            log.info("ACM Score updated for User {}: +1 point, Penalty +{}", submission.getUser().getUsername(), penaltyForThisProblem);
        }
    }
}