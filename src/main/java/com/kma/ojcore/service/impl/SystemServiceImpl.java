package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.enums.SupportedLanguage;
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
        return languageLoader.getConfigs().stream()
                .map(c -> new LanguageSdo(c.getKey().name(), c.getDisplayName(), c.getAceMode()))
                .collect(Collectors.toList());
    }

    @Override
    public LanguageConfig getConfigByLang(SupportedLanguage lang) {
        return languageLoader.getConfigs().stream()
                .filter(c -> c.getKey() == lang)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ngôn ngữ không được hỗ trợ"));
    }
}
