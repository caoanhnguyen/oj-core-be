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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final UserRepository userRepository;

    @Override
    public Page<UserRankSdo> getRanking(RuleType ruleType, Pageable pageable) {

        Page<UserRepository.UserRankingProjection> ranking;

        if(ruleType == RuleType.ACM) {
            ranking = userRepository.getGlobalRankingACM(pageable);
        } else if(ruleType == RuleType.OI) {
            ranking = userRepository.getGlobalRankingOI(pageable);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid rule type: " + ruleType);
        }
        return ranking.map(projection -> new UserRankSdo(
                UUID.nameUUIDFromBytes(projection.getUserId()),
                projection.getUsername(),
                projection.getAvatarUrl(),
                projection.getAcCount(),
                projection.getSolvedCount(),
                projection.getSubmissionCount(),
                projection.getTotalScore(),
                projection.getRank()
        ));
    }
}