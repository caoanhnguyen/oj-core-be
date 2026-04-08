package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.problems.TemplateSdi;
import com.kma.ojcore.entity.ProblemTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

    @Mapping(target = "problem", ignore = true)
    ProblemTemplate toEntity(TemplateSdi template);
}
