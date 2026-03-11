package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.submissions.SubmissionSdi;

import java.util.UUID;

public interface SubmissionService {

    UUID submitCode(SubmissionSdi request, UUID currentUserId);
}
