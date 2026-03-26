package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
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
}