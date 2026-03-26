package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.RunCodeRequest;
import com.kma.ojcore.dto.request.submissions.RunCodeSubmitDto;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.service.RunCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunCodeServiceImpl implements RunCodeService {

    private final ProblemRepository problemRepository;
    private final LanguageLoader languageLoader;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public UUID sendToJudge(RunCodeSubmitDto request) {

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        if (!problem.getAllowedLanguages().contains(request.getLanguageKey())) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }

        LanguageConfig langConfig = languageLoader.getConfigByKey(request.getLanguageKey());
        if (langConfig == null) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }

        int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier()) + langConfig.getTimeLimitAllowance();
        int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier()) + langConfig.getMemoryLimitAllowance();

        final UUID runToken = UUID.randomUUID();

        RunCodeRequest judgeRequest = RunCodeRequest.builder()
                .runToken(runToken)
                .problemId(request.getProblemId())
                .sourceCode(request.getSourceCode())
                .languageKey(request.getLanguageKey())
                .customInputs(request.getCustomInputs())
                .compileCommand(langConfig.getCompileCommand())
                .runCommand(langConfig.getRunCommand())
                .isCompiled(langConfig.isCompiled())
                .sourceName(langConfig.getSourceName())
                .exeName(langConfig.getExeName())
                .finalTimeLimitMs(finalTimeLimit)
                .finalMemoryLimitMb(finalMemoryLimit)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.JUDGE_EXCHANGE,
                RabbitMQConfig.RUN_CODE_ROUTING_KEY,
                judgeRequest
        );

        log.info("Sent Run Code request [{}] to RabbitMQ", runToken);
        return runToken;
    }
}