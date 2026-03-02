package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.ProgrammingLanguage;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TemplateSdi {
    ProgrammingLanguage language;
    String codeTemplate;
}
