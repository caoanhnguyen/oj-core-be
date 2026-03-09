package com.kma.ojcore.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LanguageSdo {
    private String languageKey;
    private String displayName;
    private String aceMode;
}
