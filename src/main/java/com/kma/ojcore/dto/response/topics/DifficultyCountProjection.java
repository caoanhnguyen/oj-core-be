package com.kma.ojcore.dto.response.topics;

import com.kma.ojcore.enums.ProblemDifficulty;

public interface DifficultyCountProjection {
    ProblemDifficulty getDifficulty();
    Long getCount();
}