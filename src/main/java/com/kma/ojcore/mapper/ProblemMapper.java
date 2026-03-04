package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo.ProblemTemplateSummary;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo.TestCaseSummary;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.ProblemTemplate;
import com.kma.ojcore.entity.TestCase;
import com.kma.ojcore.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProblemMapper {

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "templates", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "images", ignore = true)
    Problem toEntity(CreateProblemSdi request);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "templates", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntityFromRequest(CreateProblemSdi request, @MappingTarget Problem problem);

    ProblemResponse toProblemResponse(Problem problem);

    @Mapping(target = "templates", expression = "java(toTemplateSummaries(problem.getTemplates()))")
    @Mapping(target = "testCases", expression = "java(toTestCaseSummaries(problem.getTestCases()))")
    @Mapping(target = "examples", expression = "java(toExampleSummaries(problem.getExamples()))")
    @Mapping(target = "topics", expression = "java(toTopicSummaries(problem.getTopics()))")
    ProblemDetailsSdo toProblemDetailsSdo(Problem problem);

    default List<ProblemTemplateSummary> toTemplateSummaries(List<ProblemTemplate> templates) {
        if (templates == null) {
            return null;
        }
        return templates.stream()
                .map(t -> ProblemTemplateSummary.builder()
                        .id(t.getId())
                        .language(t.getLanguage().name())
                        .codeTemplate(t.getCodeTemplate())
                        .build())
                .collect(Collectors.toList());
    }

    default List<TestCaseSummary> toTestCaseSummaries(List<TestCase> testCases) {
        if (testCases == null) {
            return null;
        }
        return testCases.stream()
                .map(tc -> TestCaseSummary.builder()
                        .id(tc.getId())
                        .isSample(tc.isSample())
                        .isHidden(tc.isHidden())
                        .orderIndex(tc.getOrderIndex())
                        .illustrationUrl(tc.getIllustrationUrl())
                        .inputData(tc.isSample() ? tc.getInputData() : null)
                        .outputData(tc.isSample() ? tc.getOutputData() : null)
                        .inputUrl(tc.isSample() ? tc.getInputUrl() : null)
                        .outputUrl(tc.isSample() ? tc.getOutputUrl() : null)
                        .build())
                .collect(Collectors.toList());
    }

    default List<ProblemDetailsSdo.ExampleSummary> toExampleSummaries(
            List<com.kma.ojcore.entity.ProblemExample> examples) {
        if (examples == null) {
            return null;
        }
        return examples.stream()
                .map(ex -> ProblemDetailsSdo.ExampleSummary.builder()
                        .id(ex.getId())
                        .inputData(ex.getInputData())
                        .outputData(ex.getOutputData())
                        .explanation(ex.getExplanation())
                        .orderIndex(ex.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
    }

    default List<ProblemDetailsSdo.TopicsSummary> toTopicSummaries(
            Set<Topic> topics) {
        if (topics == null) {
            return null;
        }
        return topics.stream()
                .map(ex -> ProblemDetailsSdo.TopicsSummary.builder()
                        .topicId(ex.getId())
                        .name(ex.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
