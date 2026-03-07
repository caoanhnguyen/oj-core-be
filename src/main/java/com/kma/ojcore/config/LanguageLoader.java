package com.kma.ojcore.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.entity.LanguageConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component này sẽ tự động load file languages.json từ resources khi ứng dụng khởi động
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class LanguageLoader {
    private final List<LanguageConfig> configs = new ArrayList<>();
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/languages.json")) {
            if (is == null) throw new IOException("Config file not found");
            List<LanguageConfig> data = objectMapper.readValue(is, new TypeReference<List<LanguageConfig>>() {});
            configs.addAll(data);
            log.info("Successfully loaded {} languages", configs.size());
        } catch (IOException e) {
            log.error("Failed to load languages.json: {}", e.getMessage());
        }
    }

    public List<LanguageConfig> getConfigs() {
        return Collections.unmodifiableList(configs);
    }
}