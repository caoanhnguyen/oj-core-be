package com.kma.ojcore.entity;

import com.kma.ojcore.enums.SupportedLanguage;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class LanguageConfig {
    SupportedLanguage key;
    String displayName;
    String aceMode;
    String compileCommand;
    String runCommand;
    boolean isCompiled;
    String sourceName;
    String exeName;
    Integer memoryLimitAllowance;
    Integer timeLimitAllowance;
}