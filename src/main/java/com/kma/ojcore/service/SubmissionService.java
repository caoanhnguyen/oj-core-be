package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kma.ojcore.dto.request.submissions.RejudgeSdi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SubmissionService {

    UUID submitCode(SubmissionSdi request, UUID currentUserId);

    SubmissionDetailsSdo getSubmissionBasicInfo(UUID submissionId);

    RunCodeResponse getRunCodeResult(UUID runCodeToken);

    Page<?> getSubmissions(UUID problemId,
                           UUID userId,
                           SubmissionVerdict submissionVerdict,
                           String keyword,
                           EStatus status,
                           ProblemStatus problemStatus,
                           EStatus submissionStatus,
                           String languageKey,
                           LocalDateTime fromDate,
                           LocalDateTime toDate,
                           List<SubmissionVerdict> allowedVerdicts,
                           boolean hideStaff,
                           boolean ignoreContestPrivacy,
                           boolean isPracticeOnly,
                           Pageable pageable);

    ProblemStatisticSdo getProblemStatistics(UUID problemId, List<SubmissionVerdict> allowedVerdicts);

    String getLatestSubmissionCode(UUID problemId, UUID userId, String languageKey);

    void rejudgeSubmissions(RejudgeSdi request);

    void softDeleteSubmissions(List<UUID> ids);

    void voidSubmissions(List<UUID> ids);

    void restoreSubmissions(List<UUID> ids);

    List<com.kma.ojcore.dto.response.submissions.SubmissionStatusSdo> checkSubmissionStatuses(List<UUID> ids);
}
