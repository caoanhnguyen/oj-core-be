package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.ProblemExample;
import com.kma.ojcore.entity.ProblemTemplate;
import com.kma.ojcore.entity.Topic;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.exception.ResourceAlreadyExistsException;
import com.kma.ojcore.exception.ResourceNotFoundException;
import com.kma.ojcore.mapper.ExampleMapper;
import com.kma.ojcore.mapper.ProblemMapper;
import com.kma.ojcore.mapper.TemplateMapper;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.TopicRepository;
import com.kma.ojcore.service.ImageStorageService;
import com.kma.ojcore.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemMapper problemMapper;
    private final TemplateMapper templateMapper;
    private final ExampleMapper exampleMapper;
    private final ImageStorageService imageStorageService;
    private final TopicRepository topicRepository;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ProblemDetailsSdo createProblem(CreateProblemSdi request) throws BadRequestException {
        log.info("Creating problem with slug: {}", request.getSlug());

        // 1. Validate slug uniqueness
        if (problemRepository.existsBySlug(request.getSlug())) {
            throw new ResourceAlreadyExistsException("Problem slug already exists");
        }

        // 2. Map to entity
        Problem problem = problemMapper.toEntity(request);

        // 3. Save Problem first (để có ID cho việc commit images)
        Problem saved = problemRepository.save(problem);
        log.info("Problem saved with ID: {}", saved.getId());

        // 4. Commit images nếu có
        if (request.getTemporaryImageKeys() != null && !request.getTemporaryImageKeys().isEmpty()) {
            log.info("Committing {} images", request.getTemporaryImageKeys().size());

            Map<String, String> urlMapping = imageStorageService.commitImages(
                    request.getTemporaryImageKeys(),
                    saved);

            // 5. Replace URLs trong description, constraints
            if (!urlMapping.isEmpty()) {
                saved.setDescription(replaceImageUrls(saved.getDescription(), urlMapping));
                saved.setConstraints(replaceImageUrls(saved.getConstraints(), urlMapping));
            }
        }

        // 6. Tạo Examples nếu có
        if (request.getExamples() != null && !request.getExamples().isEmpty()) {
            log.info("Creating {} examples", request.getExamples().size());

            List<ProblemExample> examples = request.getExamples().stream()
                    .map(exampleSdi -> {
                        ProblemExample example = exampleMapper.toEntity(exampleSdi);
                        example.setProblem(saved);
                        return example;
                    })
                    .collect(Collectors.toList());

            saved.setExamples(examples);
        }

        // 7. Tạo Templates nếu có
        if (request.getTemplates() != null && !request.getTemplates().isEmpty()) {
            log.info("Creating {} templates", request.getTemplates().size());

            List<ProblemTemplate> templates = new ArrayList<>();
            request.getTemplates().forEach(templateSdi -> {
                ProblemTemplate template = templateMapper.toEntity(templateSdi);
                templates.add(template);
            });
            final Problem finalSaved = saved; // Create final reference for lambda
            templates.forEach(t -> t.setProblem(finalSaved));
            saved.setTemplates(templates);
        }

        // 8. Lưu danh sách topics nếu có
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            log.info("Associating {} topics", request.getTopicIds().size());
            // Check Topics tồn tại và status là active
            List<Topic> topics = topicRepository.findByIdInAndStatus(request.getTopicIds(), EStatus.ACTIVE);

            // Nếu số lượng trả về từ repo khác với request -> có topic không tồn tại hoặc không active
            if(topics.size() != request.getTopicIds().size()) {
                throw new BadRequestException("One or more topics not found or not active");
            }

            // Thêm list topic cho problem
            saved.setTopics(new HashSet<>(topics));
        }

        // 9. Save lại Problem với các mối quan hệ đã thiết lập
        Problem finalProblem = problemRepository.save(saved);
        log.info("Problem created successfully: {}", finalProblem.getId());

        return problemMapper.toProblemDetailsSdo(finalProblem);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailsSdo getProblemById(UUID id) {
        Problem problem = problemRepository.findByIdAndStatus(id, EStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));
        return problemMapper.toProblemDetailsSdo(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailsSdo getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlugAndStatus(slug, EStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with slug: " + slug));
        return problemMapper.toProblemDetailsSdo(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProblemResponse> getProblems(ProblemDifficulty difficulty, String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Problem> page;
        if (difficulty != null && searchKeyword != null) {
            page = problemRepository.findByStatusAndDifficultyAndTitleContainingIgnoreCase(EStatus.ACTIVE, difficulty,
                    searchKeyword, pageable);
        } else if (difficulty != null) {
            page = problemRepository.findByStatusAndDifficulty(EStatus.ACTIVE, difficulty, pageable);
        } else if (searchKeyword != null) {
            page = problemRepository.findByStatusAndTitleContainingIgnoreCase(EStatus.ACTIVE, searchKeyword, pageable);
        } else {
            page = problemRepository.findByStatus(EStatus.ACTIVE, pageable);
        }

        return page.map(problemMapper::toProblemResponse);
    }

    @Override
    @Transactional
    public ProblemDetailsSdo updateProblem(UUID id, CreateProblemSdi request) {
        log.info("Updating problem: {}", id);

        Problem problem = problemRepository.findByIdAndStatus(id, EStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));

        // Validate slug if changed
        if (!problem.getSlug().equals(request.getSlug()) && problemRepository.existsBySlug(request.getSlug())) {
            throw new ResourceAlreadyExistsException("Problem slug already exists");
        }

        // Update basic fields
        problemMapper.updateEntityFromRequest(request, problem);

        // Commit new images nếu có
        if (request.getTemporaryImageKeys() != null && !request.getTemporaryImageKeys().isEmpty()) {
            log.info("Committing {} new images for update", request.getTemporaryImageKeys().size());

            Map<String, String> urlMapping = imageStorageService.commitImages(
                    request.getTemporaryImageKeys(),
                    problem);

            // Replace URLs trong description, constraints
            if (!urlMapping.isEmpty()) {
                problem.setDescription(replaceImageUrls(problem.getDescription(), urlMapping));
                problem.setConstraints(replaceImageUrls(problem.getConstraints(), urlMapping));
            }
        }

        // Update Examples: Clear old and add new
        if (request.getExamples() != null) {
            log.info("Updating examples");

            // Clear old examples (orphanRemoval will delete them)
            if (problem.getExamples() != null) {
                problem.getExamples().clear();
            } else {
                problem.setExamples(new ArrayList<>());
            }

            // Add new examples
            request.getExamples().forEach(exampleSdi -> {
                ProblemExample example = exampleMapper.toEntity(exampleSdi);
                example.setProblem(problem);
                problem.getExamples().add(example);
            });
        }

        // Update Templates: Clear old and add new
        if (request.getTemplates() != null) {
            log.info("Updating templates");

            // Clear old templates
            if (problem.getTemplates() != null) {
                problem.getTemplates().clear();
            } else {
                problem.setTemplates(new ArrayList<>());
            }

            // Add new templates
            request.getTemplates().forEach(templateSdi -> {
                ProblemTemplate template = templateMapper.toEntity(templateSdi);
                template.setProblem(problem);
                problem.getTemplates().add(template);
            });
        }

        Problem saved = problemRepository.save(problem);
        log.info("Problem updated successfully: {}", id);

        return problemMapper.toProblemDetailsSdo(saved);
    }

    /**
     * Xóa mềm một Problem bằng cách đặt trạng thái của nó thành DELETED.
     * Cũng xóa tất cả ảnh liên quan.
     * 
     * @param id UUID của Problem cần xóa
     */
    @Override
    @Transactional
    public void deleteProblem(UUID id) {
        log.info("Deleting problem: {}", id);

        Problem problem = problemRepository.findByIdAndStatus(id, EStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));

        // Delete all images associated with this problem
        imageStorageService.deleteAllImagesForProblem(id);

        // Soft delete problem
        problem.setStatus(EStatus.DELETED);
        problemRepository.save(problem);

        log.info("Problem deleted successfully: {}", id);
    }

    // ========== Helper Methods ========== //

    /**
     * Replace image URLs trong HTML content
     * 
     * @param htmlContent HTML content chứa img tags
     * @param urlMapping  Map từ old URL sang new URL
     * @return HTML content với URLs đã được thay thế
     */
    private String replaceImageUrls(String htmlContent, Map<String, String> urlMapping) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        String result = htmlContent;
        for (Map.Entry<String, String> entry : urlMapping.entrySet()) {
            // Replace all occurrences of old URL with new URL
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
