package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.SupportedLanguage;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TemplateSdi {
    SupportedLanguage language;
    String codeTemplate;
}
