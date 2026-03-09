package com.kma.ojcore.dto.request.problems;

import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TemplateSdi {
    String languageKey;
    String codeTemplate;
}
