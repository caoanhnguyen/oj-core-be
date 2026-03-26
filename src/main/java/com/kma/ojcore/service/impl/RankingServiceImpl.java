package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.response.users.UserRankSdo;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final UserRepository userRepository;

    @Override
    public Page<UserRankSdo> getRanking(RuleType ruleType, Pageable pageable) {

        if(ruleType == RuleType.ACM) {
            return userRepository.getACMRanking(pageable);
        } else if(ruleType == RuleType.OI) {
            return userRepository.getOIRanking(pageable);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid rule type: " + ruleType);
        }
    }
}