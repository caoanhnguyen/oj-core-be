package com.kma.ojcore.service.scoring;

import com.kma.ojcore.entity.ContestParticipation;
import com.kma.ojcore.entity.Submission;

public interface ContestScoringStrategy {
    void processScore(Submission submission, ContestParticipation participation);
}