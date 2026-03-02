package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.problems.ExampleSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.entity.ProblemExample;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper cho ProblemExample entity
 */
@Mapper(componentModel = "spring")
public interface ExampleMapper {

    /**
     * Convert từ ExampleSdi sang ProblemExample entity
     * MapStruct sẽ tự động ignore các field không có trong source DTO
     */
    @Mapping(target = "problem", ignore = true)
    ProblemExample toEntity(ExampleSdi sdi);

    /**
     * Convert list
     */
    List<ProblemExample> toEntity(List<ExampleSdi> sdis);

    /**
     * Convert từ ProblemExample entity sang ExampleSummary DTO
     */
    ProblemDetailsSdo.ExampleSummary toSummary(ProblemExample entity);

    /**
     * Convert list
     */
    List<ProblemDetailsSdo.ExampleSummary> toSummary(List<ProblemExample> entities);
}
