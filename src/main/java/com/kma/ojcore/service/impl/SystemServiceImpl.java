package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {
    private final LanguageLoader languageLoader;

    @Override
    public List<LanguageSdo> getSupportedLanguages() {
        // getAllConfigs() giờ trả về Map, ta lấy values() để map ra List cho Frontend
        return languageLoader.getAllConfigs().values().stream()
                // getKey() giờ đã là String rồi, nên không cần .name() như hồi dùng Enum nữa
                .map(c -> new LanguageSdo(c.getLanguageKey(), c.getDisplayName(), c.getAceMode()))
                .collect(Collectors.toList());
    }

    // Tôi mở comment và sửa lại hàm này cho bro luôn,
    // vì lúc xử lý nộp bài (Submission), bro CẦN hàm này để lấy config tính toán thời gian/RAM
    @Override
    public LanguageConfig getConfigByKey(String languageKey) {
        LanguageConfig config = languageLoader.getConfigByKey(languageKey);

        if (config == null) {
            throw new BusinessException("Ngôn ngữ không được hệ thống hỗ trợ: " + languageKey);
        }

        return config;
    }


}