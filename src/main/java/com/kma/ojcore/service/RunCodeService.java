package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.submissions.RunCodeSubmitDto;

import java.util.UUID;

public interface RunCodeService {
    UUID sendToJudge(RunCodeSubmitDto request);
}