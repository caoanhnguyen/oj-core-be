package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.UserRankSdo;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RankingService {

    Page<UserRankSdo> getRanking(RuleType ruleType, Pageable pageable);
}
