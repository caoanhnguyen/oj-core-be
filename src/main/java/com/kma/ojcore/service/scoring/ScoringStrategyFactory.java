package com.kma.ojcore.service.scoring;

import com.kma.ojcore.enums.RuleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScoringStrategyFactory {

    private final AcmScoringStrategy acmScoringStrategy;
    private final OiScoringStrategy oiScoringStrategy;

    public ContestScoringStrategy getStrategy(RuleType ruleType) {
        return switch (ruleType) {
            case ACM -> acmScoringStrategy;
            case OI -> oiScoringStrategy;
            default -> throw new IllegalArgumentException("Unsupported rule type: " + ruleType);
        };
    }
}