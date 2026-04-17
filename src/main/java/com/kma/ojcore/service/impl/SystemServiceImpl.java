package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.dto.response.common.DashboardStatsSdo;
import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.ContestRepository;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {
    private final LanguageLoader languageLoader;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public List<LanguageSdo> getSupportedLanguages() {
        return languageLoader.getAllConfigs().values().stream()
                .map(c -> new LanguageSdo(c.getLanguageKey(), c.getDisplayName(), c.getAceMode()))
                .collect(Collectors.toList());
    }

    @Override
    public LanguageConfig getConfigByKey(String languageKey) {
        LanguageConfig config = languageLoader.getConfigByKey(languageKey);

        if (config == null) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }

        return config;
    }

    @Override
    public DashboardStatsSdo getAdminDashboardStats(Integer days) {
        if (days == null || days <= 0) days = 7;
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        long totalProblems = problemRepository.countByStatusNot(EStatus.DELETED);
        long activeUsers = userRepository.countByAccountNonLockedTrue();
        long activeContests = contestRepository.countByStatusNot(EStatus.DELETED);
        long totalSubmissions = submissionRepository.countByStatusNot(EStatus.DELETED);

        List<DashboardStatsSdo.VerdictStat> verdictStats = submissionRepository.countVerdictsByStartDate(startDate)
                .stream()
                .map(v -> new DashboardStatsSdo.VerdictStat(v.getVerdict(), v.getCount()))
                .collect(Collectors.toList());

        List<DashboardStatsSdo.TrendStat> trendStats = submissionRepository.countTrendsByStartDate(startDate)
                .stream()
                .map(t -> new DashboardStatsSdo.TrendStat(t.getDateStr(), t.getCount()))
                .collect(Collectors.toList());

        return DashboardStatsSdo.builder()
                .totalProblems(totalProblems)
                .activeUsers(activeUsers)
                .activeContests(activeContests)
                .totalSubmissions(totalSubmissions)
                .verdictStats(verdictStats)
                .trendStats(trendStats)
                .build();
    }
}