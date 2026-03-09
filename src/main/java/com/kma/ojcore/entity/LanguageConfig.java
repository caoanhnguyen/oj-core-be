package com.kma.ojcore.entity;

import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class LanguageConfig {
    String languageKey;
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