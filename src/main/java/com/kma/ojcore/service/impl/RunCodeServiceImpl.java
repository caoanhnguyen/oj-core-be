package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.RunCodeRequest;
import com.kma.ojcore.dto.request.submissions.RunCodeSubmitDto;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.exception.BusinessException;
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

        // 1 & 2: Tìm Problem và Validate
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy bài toán!"));

        if (!problem.getAllowedLanguages().contains(request.getLanguageKey())) {
            throw new BusinessException("Ngôn ngữ " + request.getLanguageKey() + " không được hỗ trợ!");
        }

        // Lấy cấu hình ngôn ngữ từ LanguageLoader
        LanguageConfig langConfig = languageLoader.getConfigByKey(request.getLanguageKey());
        if (langConfig == null) {
            throw new BusinessException("Hệ thống chưa cấu hình ngôn ngữ này!");
        }

        // --- 3. TÍNH TOÁN FINAL LIMIT VÀ ĐÓNG GÓI RunCodeRequest ---
        // Công thức: Final = (Base * Multiplier) + Allowance
        int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier()) + langConfig.getTimeLimitAllowance();
        int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier()) + langConfig.getMemoryLimitAllowance();

        final UUID runToken = UUID.randomUUID(); // Tạo một Token ngẫu nhiên để định danh cho lần chạy code này

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

        // 4. Bắn vào RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.JUDGE_EXCHANGE,
                RabbitMQConfig.RUN_CODE_ROUTING_KEY,
                judgeRequest
        );

        log.info("Đã gửi yêu cầu Run Code [{}] vào RabbitMQ", runToken);

        // 5. Trả Token về cho Controller
        return runToken;
    }
}