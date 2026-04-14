package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.request.problems.UpdateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.entity.*;
import com.kma.ojcore.enums.*;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.ExampleMapper;
import com.kma.ojcore.mapper.ProblemMapper;
import com.kma.ojcore.mapper.TemplateMapper;
import com.kma.ojcore.repository.*;
import com.kma.ojcore.service.ImageStorageService;
import com.kma.ojcore.service.ProblemService;
import com.kma.ojcore.utils.EscapeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserProblemStatusRepository userProblemStatusRepo;
    private final UserRepository userRepository;
    private final ContestProblemRepository contestProblemRepository;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ProblemDetailsSdo createProblem(CreateProblemSdi request, UUID currentUserId) {
        log.info("Creating problem with slug: {}", request.getSlug());

        if (problemRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.PROBLEM_ALREADY_EXISTS);
        }

        Problem problem = problemMapper.toEntity(request);
        problem.setProblemStatus(ProblemStatus.DRAFT);
        problem.setAcceptedCount(0L);
        problem.setSubmissionCount(0L);

        User author = userRepository.getReferenceById(currentUserId);
        problem.setAuthor(author);
        Problem saved = problemRepository.save(problem);
        log.info("Problem saved with ID: {}", saved.getId());

        if (request.getTemporaryImageKeys() != null && !request.getTemporaryImageKeys().isEmpty()) {
            log.info("Committing {} images", request.getTemporaryImageKeys().size());
            imageStorageService.commitImages(request.getTemporaryImageKeys(), saved);
        }

        if (request.getExamples() != null && !request.getExamples().isEmpty()) {
            List<ProblemExample> examples = request.getExamples().stream()
                    .map(exampleSdi -> {
                        ProblemExample example = exampleMapper.toEntity(exampleSdi);
                        example.setProblem(saved);
                        return example;
                    })
                    .collect(Collectors.toList());
            saved.setExamples(examples);
        }

        if (request.getTemplates() != null && !request.getTemplates().isEmpty()) {
            List<ProblemTemplate> templates = new ArrayList<>();
            request.getTemplates().forEach(templateSdi -> {
                ProblemTemplate template = templateMapper.toEntity(templateSdi);
                templates.add(template);
            });
            templates.forEach(t -> t.setProblem(saved));
            saved.setTemplates(templates);
        }

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            List<Topic> topics = topicRepository.findByIdInAndStatus(request.getTopicIds(), EStatus.ACTIVE);
            if (topics.size() != request.getTopicIds().size()) {
                throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND, "One or more topics not found or not active");
            }
            saved.setTopics(new HashSet<>(topics));
        }

        Problem finalProblem = problemRepository.save(saved);
        log.info("Problem created successfully: {}", finalProblem.getId());

        return problemMapper.toProblemDetailsSdo(finalProblem);
    }

    @Transactional(readOnly = true)
    @Override
    public ProblemDetailsSdo getProblemById(UUID id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        return problemMapper.toProblemDetailsSdo(problem);
    }

    @Transactional(readOnly = true)
    @Override
    public ProblemDetailsSdo getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
        
        if (problem.getStatus() != EStatus.ACTIVE || problem.getProblemStatus() != ProblemStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND, "Problem is not available or inactive");
        }
        
        return problemMapper.toProblemDetailsSdo(problem);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProblemResponse> getProblems(String keyword,
                                             ProblemDifficulty difficulty,
                                             RuleType ruleType,
                                             List<String> topicSlugs,
                                             EStatus status,
                                             ProblemStatus problemStatus,
                                             UUID userId,
                                             UUID contestId, // <--- Nhận contestId từ Controller
                                             Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        Page<ProblemResponse> pageResult = problemRepository.searchProblems(searchKeyword,
                difficulty, ruleType, topicSlugs, status, problemStatus, pageable);

        if (pageResult.isEmpty()) {
            return pageResult;
        }

        // Lấy danh sách problemIds của Page hiện tại (Dùng chung cho cả 2 luồng)
        List<UUID> problemIds = pageResult.getContent().stream()
                .map(ProblemResponse::getId)
                .collect(Collectors.toList());

        // =========================================================
        // LUỒNG 1: LOGIC TRẠNG THÁI LÀM BÀI CỦA USER (Cũ của bro)
        // =========================================================
        if (userId != null) {
            List<UserProblemStatus> statuses = userProblemStatusRepo.findByUserIdAndProblemIdIn(userId, problemIds);

            Map<UUID, UserProblemState> statusMap = statuses.stream()
                    .collect(Collectors.toMap(
                            s -> s.getProblem().getId(),
                            UserProblemStatus::getState
                    ));

            pageResult.getContent().forEach(problem -> {
                if (statusMap.containsKey(problem.getId())) {
                    problem.setUserProblemState(statusMap.get(problem.getId()).name());
                }
            });
        }

        // =========================================================
        // LUỒNG 2: LOGIC ĐÁNH DẤU BÀI ĐÃ THÊM VÀO CONTEST (Mới)
        // =========================================================
        if (contestId != null) {
            // Lấy danh sách ID các bài toán ĐÃ CÓ trong Contest
            List<UUID> addedProblemIds = contestProblemRepository.findProblemIdsByContestId(contestId);

            // Bỏ vào HashSet để tra cứu với tốc độ bàn thờ O(1)
            Set<UUID> addedSet = new HashSet<>(addedProblemIds);

            // Gắn cờ isAdded cho FE vẽ giao diện
            pageResult.getContent().forEach(problem -> {
                problem.setIsAdded(addedSet.contains(problem.getId()));
            });
        }

        return pageResult;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ProblemDetailsSdo updateProblem(UUID id, UpdateProblemSdi request) {
        log.info("Updating problem: {}", id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        if (!problem.getSlug().equals(request.getSlug()) && problemRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.PROBLEM_ALREADY_EXISTS);
        }

        problemMapper.updateEntityFromRequest(request, problem);

        if (request.getTemporaryImageKeys() != null) {
            log.info("Syncing {} images for problem update", request.getTemporaryImageKeys().size());
            imageStorageService.syncProblemImages(request.getTemporaryImageKeys(), problem);
        }

        if (request.getExamples() != null) {
            if (problem.getExamples() != null) {
                problem.getExamples().clear();
                problemRepository.flush();
            } else {
                problem.setExamples(new ArrayList<>());
            }

            request.getExamples().forEach(exampleSdi -> {
                ProblemExample example = exampleMapper.toEntity(exampleSdi);
                example.setProblem(problem);
                problem.getExamples().add(example);
            });
        }

        if (request.getTemplates() != null) {
            if (problem.getTemplates() != null) {
                problem.getTemplates().clear();
                problemRepository.flush();
            } else {
                problem.setTemplates(new ArrayList<>());
            }

            request.getTemplates().forEach(templateSdi -> {
                ProblemTemplate template = templateMapper.toEntity(templateSdi);
                template.setProblem(problem);
                problem.getTemplates().add(template);
            });
        }

        if (request.getTopicIds() != null) {
            List<Topic> topics = topicRepository.findByIdInAndStatus(request.getTopicIds(), EStatus.ACTIVE);
            if (topics.size() != request.getTopicIds().size()) {
                throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND, "One or more topics not found or not active");
            }
            problem.getTopics().clear();
            problem.getTopics().addAll(topics);
        }

        Problem saved = problemRepository.save(problem);
        log.info("Problem updated successfully: {}", id);

        return problemMapper.toProblemDetailsSdo(saved);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteProblem(UUID id) {
        log.info("Deleting problem: {}", id);
        if (!problemRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        if (contestProblemRepository.existsByProblemId(id)) {
            log.warn("Cannot delete problem {} because it is currently used in a contest", id);
            throw new BusinessException(ErrorCode.PROBLEM_IN_USE, "Problem is currently used in a contest and cannot be deleted");
        }
        problemRepository.updateStatusById(EStatus.DELETED, id);
        log.info("Problem deleted successfully: {}", id);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void restoreProblem(UUID id) {
        log.info("Restoring problem: {}", id);
        if (!problemRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        problemRepository.updateStatusById(EStatus.ACTIVE, id);
        log.info("Problem restored successfully: {}", id);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void publishProblem(UUID id) {
        log.info("Publishing problem: {}", id);
        if (!problemRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }
        problemRepository.updateProblemStatusById(ProblemStatus.PUBLISHED, id);
        log.info("Problem published successfully: {}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public long countUserProblemsByUserIdAndState(UUID userId, UserProblemState state) {
        return userProblemStatusRepo.countByUserIdAndState(userId, state);
    }
}