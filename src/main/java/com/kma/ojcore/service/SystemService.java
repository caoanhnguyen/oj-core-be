package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.enums.SupportedLanguage;

import java.util.List;

public interface SystemService {

    List<LanguageSdo> getSupportedLanguages();

    LanguageConfig getConfigByLang(SupportedLanguage lang);
}
