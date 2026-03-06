package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.request.problems.UpdateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo.ProblemTemplateSummary;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.ProblemTemplate;
import com.kma.ojcore.entity.Topic;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProblemMapper {

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "templates", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "topics", ignore = true)
    Problem toEntity(CreateProblemSdi request);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "templates", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateProblemSdi request, @MappingTarget Problem problem);

    ProblemResponse toProblemResponse(Problem problem);

    @Mapping(target = "authorName", source = "author.username") // Map tên tác giả
    @Mapping(target = "templates", expression = "java(toTemplateSummaries(problem.getTemplates()))")
    @Mapping(target = "examples", expression = "java(toExampleSummaries(problem.getExamples()))")
    @Mapping(target = "topics", expression = "java(toTopicSummaries(problem.getTopics()))")
    ProblemDetailsSdo toProblemDetailsSdo(Problem problem);

    default List<ProblemTemplateSummary> toTemplateSummaries(List<ProblemTemplate> templates) {
        if (templates == null) return null;
        return templates.stream()
                .map(t -> ProblemTemplateSummary.builder()
                        .id(t.getId())
                        .language(t.getLanguage()) // Bổ sung map Enum language
                        .codeTemplate(t.getCodeTemplate())
                        .build())
                .collect(Collectors.toList());
    }

    default List<ProblemDetailsSdo.ExampleSummary> toExampleSummaries(
            List<com.kma.ojcore.entity.ProblemExample> examples) {
        if (examples == null) return null;
        return examples.stream()
                .map(ex -> ProblemDetailsSdo.ExampleSummary.builder()
                        .id(ex.getId())
                        .rawInput(ex.getRawInput())   // Đã sửa tên field chuẩn
                        .rawOutput(ex.getRawOutput()) // Đã sửa tên field chuẩn
                        .explanation(ex.getExplanation())
                        .orderIndex(ex.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    default List<ProblemDetailsSdo.TopicsSummary> toTopicSummaries(Set<Topic> topics) {
        if (topics == null) return null;
        return topics.stream()
                .map(t -> ProblemDetailsSdo.TopicsSummary.builder()
                        .topicId(t.getId())
                        .name(t.getName())
                        .build())
                .collect(Collectors.toList());
    }
}