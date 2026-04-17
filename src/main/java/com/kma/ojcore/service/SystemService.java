package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.common.DashboardStatsSdo;
import com.kma.ojcore.dto.response.common.LanguageSdo;
import com.kma.ojcore.entity.LanguageConfig;

import java.util.List;

public interface SystemService {

    List<LanguageSdo> getSupportedLanguages();

    LanguageConfig getConfigByKey(String languageKey);

    DashboardStatsSdo getAdminDashboardStats(Integer days);
}
