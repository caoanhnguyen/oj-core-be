package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SubmissionService {

    UUID submitCode(SubmissionSdi request, UUID currentUserId);

    SubmissionDetailsSdo getSubmissionBasicInfo(UUID submissionId);

    RunCodeResponse getRunCodeResult(UUID runCodeToken);

    Page<?> getSubmissions(UUID problemId,
                           UUID userId,
                           SubmissionVerdict submissionVerdict,
                           String username,
                           EStatus status,
                           ProblemStatus problemStatus,
                           List<SubmissionVerdict> allowedVerdicts,
                           boolean hideStaff,
                           Pageable pageable);

    ProblemStatisticSdo getProblemStatistics(UUID problemId, List<SubmissionVerdict> allowedVerdicts);

    String getLatestSubmissionCode(UUID problemId, UUID userId, String languageKey);
}
