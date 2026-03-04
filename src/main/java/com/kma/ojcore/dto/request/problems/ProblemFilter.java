package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemFilter {
    ProblemStatus problemStatus;
    EStatus status;
    ProblemDifficulty difficulty;
    List<String> topicSlugs;
}
